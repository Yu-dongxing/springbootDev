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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.yuxs.springbootdev.core.enums.SocialPlatformEnum;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.core.utils.StpUserUtil;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.entity.SysUserSocial;
import top.yuxs.springbootdev.modules.system.mapper.SysUserSocialMapper;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;
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

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public AuthRequest getAuthRequest(String source) {
        SocialPlatformEnum platform = SocialPlatformEnum.getByCode(source);
        if (platform == null) {
            throw new BusinessException("系统暂未适配第三方平台: " + source);
        }

        // 1. 动态从系统通用配置参数表中拉取
        String clientId = sysConfigService.getValue(platform.getClientIdKey());
        String clientSecret = sysConfigService.getValue(platform.getClientSecretKey());
        String redirectUri = sysConfigService.getValue(platform.getRedirectUriKey());
        String enabled = sysConfigService.getValue(platform.getEnabledKey(), "false");

        // 2. 校验配置是否开启且参数完整
        if (!"true".equalsIgnoreCase(enabled)) {
            throw new BusinessException(platform.getName() + " 第三方登录通道已被系统管理员关闭");
        }
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret) || !StringUtils.hasText(redirectUri)) {
            throw new BusinessException("系统未完整配置 " + platform.getName() + " 授权秘钥及回调参数");
        }

        // 3. 构建对应的 JustAuth 授权对象
        switch (platform) {
            case GITHUB:
                return new AuthGithubRequest(AuthConfig.builder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .redirectUri(redirectUri)
                        .build());
            case GITEE:
                return new AuthGiteeRequest(AuthConfig.builder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .redirectUri(redirectUri)
                        .build());
            default:
                throw new BusinessException("本系统暂未适配该社交平台的 Request 组装: " + platform.getName());
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
