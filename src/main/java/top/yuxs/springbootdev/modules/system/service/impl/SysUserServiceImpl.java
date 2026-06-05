/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.yuxs.springbootdev.core.config.AegisSecurityProperties;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.core.utils.StpUserUtil;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.mapper.SysUserMapper;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;
import top.yuxs.springbootdev.modules.system.service.SysUserService;

/**
 * 系统统一用户 服务实现类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AegisSecurityProperties securityProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerUser(String username, String encryptedPassword) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }
        
        // 1. 唯一性校验
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (count > 0) {
            throw new BusinessException("该用户名已被占用");
        }

        // 2. 自适应处理前端解密
        String plainPassword;
        if (securityProperties.isFrontendEncryptEnabled()) {
            plainPassword = decryptPassword(encryptedPassword);
        } else {
            plainPassword = encryptedPassword; // 禁用前端加密时，入参即为明文
        }
        
        if (!StringUtils.hasText(plainPassword) || plainPassword.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }

        // 3. 自适应处理存储加密
        String finalPassword;
        if (securityProperties.isPasswordEncryptEnabled()) {
            finalPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        } else {
            finalPassword = plainPassword; // 禁用密码存储加密，直接明文落库
        }

        // 4. 落库
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(finalPassword);
        user.setUserType("USER"); // 注册默认为 C 端普通用户
        user.setStatus(0); // 正常启用
        this.save(user);
    }

    @Override
    public String loginUser(String username, String encryptedPassword) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }

        // 1. 根据用户名以及 user_type 过滤出 C端 普通用户
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getUserType, "USER"));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("该账号已被系统禁用，无法登录");
        }

        // 2. 自适应前端解密
        String plainPassword;
        if (securityProperties.isFrontendEncryptEnabled()) {
            plainPassword = decryptPassword(encryptedPassword);
        } else {
            plainPassword = encryptedPassword;
        }

        // 3. 自适应存储哈希校验
        if (securityProperties.isPasswordEncryptEnabled()) {
            if (!BCrypt.checkpw(plainPassword, user.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }
        } else {
            if (!plainPassword.equals(user.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }
        }

        // 4. 派发 C端 会话 Token 凭证
        StpUserUtil.login(user.getId());
        return StpUserUtil.getTokenValue();
    }

    @Override
    public String loginAdmin(String username, String encryptedPassword) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }

        // 1. 根据用户名以及 user_type 过滤出 B端 管理端账号
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getUserType, "ADMIN"));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException("该管理账号已被禁用");
        }

        // 2. 自适应前端解密
        String plainPassword;
        if (securityProperties.isFrontendEncryptEnabled()) {
            plainPassword = decryptPassword(encryptedPassword);
        } else {
            plainPassword = encryptedPassword;
        }

        // 3. 自适应存储哈希校验
        if (securityProperties.isPasswordEncryptEnabled()) {
            if (!BCrypt.checkpw(plainPassword, user.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }
        } else {
            if (!plainPassword.equals(user.getPassword())) {
                throw new BusinessException("用户名或密码错误");
            }
        }

        // 4. 派发 B端 管理会话 Token 凭证 (StpUtil 隔离 C 端)
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public String decryptPassword(String encryptedPassword) {
        if (!StringUtils.hasText(encryptedPassword)) {
            throw new BusinessException("密文密码不能为空");
        }
        try {
            // 从通用参数配置表中读取 RSA 私钥进行解密
            String privateKey = sysConfigService.getValue("sys.auth.rsa.private-key");
            if (!StringUtils.hasText(privateKey)) {
                throw new BusinessException("系统未初始化高强度传输解密私钥，请联系系统管理员");
            }
            // 利用 Hutool 动态装配解密
            RSA rsa = new RSA(privateKey, null);
            return rsa.decryptStr(encryptedPassword, KeyType.PrivateKey);
        } catch (Exception e) {
            log.error(">>>>>> [Aegis Decrypt] RSA 解密前端传入的密码发生系统异常！可能格式有误或密钥过期", e);
            throw new BusinessException("账户安全校验不通过，密码格式异常");
        }
    }
}
