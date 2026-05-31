/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.core.utils.StpUserUtil;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.entity.SysUserSocial;
import top.yuxs.springbootdev.modules.system.mapper.SysUserSocialMapper;
import top.yuxs.springbootdev.modules.system.service.SysUserService;
import top.yuxs.springbootdev.modules.system.service.SysUserSocialService;

import java.util.UUID;

/**
 * 用户第三方社交账号绑定 服务实现类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Service
public class SysUserSocialServiceImpl extends ServiceImpl<SysUserSocialMapper, SysUserSocial> implements SysUserSocialService {

    @Value("${justauth.type.github.client-id:}")
    private String githubClientId;
    @Value("${justauth.type.github.client-secret:}")
    private String githubClientSecret;
    @Value("${justauth.type.github.redirect-uri:}")
    private String githubRedirectUri;

    @Value("${justauth.type.gitee.client-id:}")
    private String giteeClientId;
    @Value("${justauth.type.gitee.client-secret:}")
    private String giteeClientSecret;
    @Value("${justauth.type.gitee.redirect-uri:}")
    private String giteeRedirectUri;

    @Autowired
    private SysUserService sysUserService;

    @Override
    public AuthRequest getAuthRequest(String source) {
        if (!StringUtils.hasText(source)) {
            throw new BusinessException("第三方平台源标志不能为空");
        }
        String platform = source.toLowerCase();
        switch (platform) {
            case "github":
                if (!StringUtils.hasText(githubClientId)) {
                    throw new BusinessException("系统未配置 GitHub 第三方登录秘钥参数");
                }
                return new AuthGithubRequest(AuthConfig.builder()
                        .clientId(githubClientId)
                        .clientSecret(githubClientSecret)
                        .redirectUri(githubRedirectUri)
                        .build());
            case "gitee":
                if (!StringUtils.hasText(giteeClientId)) {
                    throw new BusinessException("系统未配置 Gitee 第三方登录秘钥参数");
                }
                return new AuthGiteeRequest(AuthConfig.builder()
                        .clientId(giteeClientId)
                        .clientSecret(giteeClientSecret)
                        .redirectUri(giteeRedirectUri)
                        .build());
            default:
                throw new BusinessException("本系统暂未适配第三方平台: " + source);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleSocialLoginOrRegister(String source, AuthUser authUser) {
        if (authUser == null || !StringUtils.hasText(authUser.getUuid())) {
            throw new BusinessException("第三方授权用户信息为空");
        }
        
        String platform = source.toUpperCase();
        String uuid = authUser.getUuid();

        // 1. 检查此第三方账户是否在本地绑定过
        LambdaQueryWrapper<SysUserSocial> queryWrapper = new LambdaQueryWrapper<SysUserSocial>()
                .eq(SysUserSocial::getSource, platform)
                .eq(SysUserSocial::getUuid, uuid);
        SysUserSocial socialBind = this.getOne(queryWrapper);

        Long userId;

        if (socialBind != null) {
            // 场景 A：已经绑定过，直接登录
            userId = socialBind.getUserId();
            SysUser existUser = sysUserService.getById(userId);
            if (existUser == null) {
                // 防御性清除脏绑定
                this.removeById(socialBind.getId());
                throw new BusinessException("关联的本地账户已丢失，请重新尝试授权注册");
            }
            if (existUser.getStatus() != null && existUser.getStatus() == 1) {
                throw new BusinessException("该关联的系统账户已被禁用，无法登录");
            }
            // 顺便把最新拉取的第三方昵称、头像同步更新（信息纠偏）
            socialBind.setNickname(authUser.getNickname());
            socialBind.setAvatar(authUser.getAvatar());
            socialBind.setRawInfo(JSON.toJSONString(authUser));
            this.updateById(socialBind);
        } else {
            // 场景 B：未绑定过，执行“一键免密注册+绑定”
            SysUser newUser = new SysUser();
            // 用户名：生成随机唯一账号
            newUser.setUsername("oauth_" + UUID.randomUUID().toString().substring(0, 8));
            // 密码：生成一个随机加密UUID作为占位，确保高度安全且无法被破解
            newUser.setPassword(UUID.randomUUID().toString());
            newUser.setUserType("USER"); // 注册为普通 C 端用户
            newUser.setStatus(0); // 账号启用
            
            sysUserService.save(newUser);
            userId = newUser.getId();

            // 保存社交绑定信息
            SysUserSocial newSocial = new SysUserSocial();
            newSocial.setUserId(userId);
            newSocial.setSource(platform);
            newSocial.setUuid(uuid);
            newSocial.setNickname(authUser.getNickname());
            newSocial.setAvatar(authUser.getAvatar());
            newSocial.setRawInfo(JSON.toJSONString(authUser));
            
            this.save(newSocial);
        }

        // 2. 执行 C端 (user) 登录态授权
        StpUserUtil.login(userId);

        // 3. 返回 C端 会话 Token 凭证给前端
        return StpUserUtil.getTokenValue();
    }
}
