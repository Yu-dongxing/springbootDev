/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/01
 */

package top.yuxs.springbootdev.modules.system.controller;

import cn.hutool.crypto.asymmetric.RSA;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.config.AegisSecurityProperties;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;
import top.yuxs.springbootdev.modules.system.service.SysUserService;

/**
 * 系统安全授权与认证 控制器
 * 提供：获取传输加密公钥、C端用户注册、C端用户登录、B端管理端登录等接口
 *
 * @author YuDongXing
 * @since 2026/06/01
 */
@Slf4j
@RestController
@RequestMapping("/api/common/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AegisSecurityProperties securityProperties;

    /**
     * 获取前端密码传输加密开启状态
     */
    @GetMapping("/frontend-encrypt/status")
    public Result<Boolean> getFrontendEncryptStatus() {
        return Result.success(securityProperties.isFrontendEncryptEnabled());
    }

    /**
     * 获取用于前端网络传输加密的 RSA 公钥
     * 极防中间人嗅探安全标准
     */
    @GetMapping("/rsa/public-key")
    public Result<String> getRsaPublicKey() {
        if (!securityProperties.isFrontendEncryptEnabled()) {
            throw new BusinessException("系统未开启前端密码传输加密服务");
        }
        String publicKey = sysConfigService.getValue("sys.auth.rsa.public-key");
        if (publicKey == null) {
            log.info(">>>>>> 数据库公钥记录为空，触发自适应热生成高安全 2048 位 RSA 密钥对...");
            RSA rsa = new RSA();
            sysConfigService.updateConfig("sys.auth.rsa.private-key", rsa.getPrivateKeyBase64(),
                    "密码安全传输 RSA 私钥", "用于解密前端用公钥加密后的密码密文，必须严防泄露");
            sysConfigService.updateConfig("sys.auth.rsa.public-key", rsa.getPublicKeyBase64(),
                    "密码安全传输 RSA 公钥", "提供给前端对传输密码原文进行非对称加密的公钥");
            publicKey = rsa.getPublicKeyBase64();
        }
        return Result.success(publicKey);
    }

    /**
     * C端普通用户端 密码安全注册
     */
    @PostMapping("/user/register")
    public Result<?> registerUser(@Validated @RequestBody AuthParam param) {
        log.info(">>>>>> 收到普通用户注册请求, 账号: {}", param.getUsername());
        sysUserService.registerUser(param.getUsername(), param.getPassword());
        return Result.success();
    }

    /**
     * C端普通用户端 密码安全登录
     */
    @PostMapping("/user/login")
    public Result<String> loginUser(@Validated @RequestBody AuthParam param) {
        log.info(">>>>>> 收到普通用户登录请求, 账号: {}", param.getUsername());
        String token = sysUserService.loginUser(param.getUsername(), param.getPassword());
        return Result.success(token);
    }

    /**
     * B端管理端 物理隔离安全登录
     */
    @PostMapping("/admin/login")
    public Result<String> loginAdmin(@Validated @RequestBody AuthParam param) {
        log.info(">>>>>> 收到管理端登录请求, 账号: {}", param.getUsername());
        String token = sysUserService.loginAdmin(param.getUsername(), param.getPassword());
        return Result.success(token);
    }

    /**
     * 内部登录参数交互 DTO 结构
     */
    @Data
    public static class AuthParam {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password; // 前端 RSA 散列强非对称密文 Base64 字符串
    }
}
