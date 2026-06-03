/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/01
 */

package top.yuxs.springbootdev.modules.system.runner;

import cn.hutool.crypto.asymmetric.RSA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.yuxs.springbootdev.core.enums.SocialPlatformEnum;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;

/**
 * 零摩擦系统配置自适应初始化与自动迁移引擎
 *
 * @author YuDongXing
 * @since 2026/06/01
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sys.config.init.enabled", havingValue = "true", matchIfMissing = true)
public class SysConfigInitRunner implements ApplicationRunner {


    @Autowired
    private SysConfigService sysConfigService;

    // 注入 YML 默认值作为备份和首次迁移的种子
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

    @Override
    public void run(ApplicationArguments args) {
        log.info(">>>>>> [Aegis Config] 开始检测并自适应持久化系统通用配置...");

        // 1. 初始化 GitHub 配置
        initSocialConfig(SocialPlatformEnum.GITHUB, githubClientId, githubClientSecret, githubRedirectUri);

        // 2. 初始化 Gitee 配置
        initSocialConfig(SocialPlatformEnum.GITEE, giteeClientId, giteeClientSecret, giteeRedirectUri);

        // 3. 初始化 微信 与 QQ (预留默认开关，默认不配置)
        initSocialConfig(SocialPlatformEnum.WECHAT, "", "", "");
        initSocialConfig(SocialPlatformEnum.QQ, "", "", "");

        // 4. 自适应检测并热初始化 RSA 加密密钥
        initRsaKeys();

        log.info(">>>>>> [Aegis Config] 系统自适应初始化配置检测完成！");
    }

    private void initSocialConfig(SocialPlatformEnum platform, String clientId, String clientSecret, String redirectUri) {
        // 如果 client-id 配置在数据库中不存在，就执行初始化更新
        String existClientId = sysConfigService.getValue(platform.getClientIdKey());
        if (existClientId == null) {
            String cId = StringUtils.hasText(clientId) ? clientId : "";
            sysConfigService.updateConfig(platform.getClientIdKey(), cId,
                    platform.getName() + " 客户端ID (Client ID)", "用于OAuth第三方授权唯一标识");
        }

        String existSecret = sysConfigService.getValue(platform.getClientSecretKey());
        if (existSecret == null) {
            String cSecret = StringUtils.hasText(clientSecret) ? clientSecret : "";
            sysConfigService.updateConfig(platform.getClientSecretKey(), cSecret,
                    platform.getName() + " 敏感密钥 (Client Secret)", "用于OAuth敏感通讯校验密钥，严禁泄露");
        }

        String existRedirect = sysConfigService.getValue(platform.getRedirectUriKey());
        if (existRedirect == null) {
            String rUri = StringUtils.hasText(redirectUri) ? redirectUri : "";
            sysConfigService.updateConfig(platform.getRedirectUriKey(), rUri,
                    platform.getName() + " 授权成功回调地址 (Redirect URI)", "第三方授权平台交互成功后回调本服务器的物理端点");
        }

        String existEnabled = sysConfigService.getValue(platform.getEnabledKey());
        if (existEnabled == null) {
            // 默认值：如果传入的 clientId 有效（不为空且不为占位符your_xx），则默认为 "true"，否则为 "false"
            boolean isDefaultActive = StringUtils.hasText(clientId) && !clientId.trim().isEmpty() && !clientId.contains("your_");
            String defaultVal = isDefaultActive ? "true" : "false";
            sysConfigService.updateConfig(platform.getEnabledKey(), defaultVal,
                    platform.getName() + " 社交登录开关", "取值 true/false，动态控制前端该登录入口的显示与启用状态");
        }
    }

    private void initRsaKeys() {
        String pubKey = sysConfigService.getValue("sys.auth.rsa.public-key");
        String privKey = sysConfigService.getValue("sys.auth.rsa.private-key");

        if (!StringUtils.hasText(pubKey) || !StringUtils.hasText(privKey)) {
            log.info(">>>>>> [Aegis Config] 检测到系统未初始化用于密码传输的 RSA 非对称公私钥对，开始自动热生成高强度 2048 位密钥对...");
            try {
                RSA rsa = new RSA();
                sysConfigService.updateConfig("sys.auth.rsa.private-key", rsa.getPrivateKeyBase64(),
                        "密码安全传输 RSA 私钥", "用于解密前端用公钥加密后的密码密文，必须严防泄露");
                sysConfigService.updateConfig("sys.auth.rsa.public-key", rsa.getPublicKeyBase64(),
                        "密码安全传输 RSA 公钥", "提供给前端对传输密码原文进行非对称加密的公钥");
                log.info(">>>>>> [Aegis Config] RSA 密码传输公私钥自动热生成并持久化成功！");
            } catch (Exception e) {
                log.error(">>>>>> [Aegis Config] 自动生成 RSA 密钥对失败！", e);
            }
        }
    }
}
