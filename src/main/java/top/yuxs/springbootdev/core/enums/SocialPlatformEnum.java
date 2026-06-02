/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/01
 */

package top.yuxs.springbootdev.core.enums;

import lombok.Getter;

/**
 * 第三方社交平台枚举定义
 *
 * @author YuDongXing
 * @since 2026/06/01
 */
@Getter
public enum SocialPlatformEnum {
    GITHUB("github", "GitHub", "justauth.type.github.client-id", "justauth.type.github.client-secret", "justauth.type.github.redirect-uri", "justauth.type.github.enabled"),
    GITEE("gitee", "Gitee", "justauth.type.gitee.client-id", "justauth.type.gitee.client-secret", "justauth.type.gitee.redirect-uri", "justauth.type.gitee.enabled"),
    WECHAT("wechat", "微信扫码", "justauth.type.wechat.client-id", "justauth.type.wechat.client-secret", "justauth.type.wechat.redirect-uri", "justauth.type.wechat.enabled"),
    QQ("qq", "QQ登录", "justauth.type.qq.client-id", "justauth.type.qq.client-secret", "justauth.type.qq.redirect-uri", "justauth.type.qq.enabled");

    private final String code;            // 前端和回调匹配的标识 (小写)
    private final String name;            // 平台友好中文名称
    private final String clientIdKey;     // 数据库对应的 Client ID 配置键
    private final String clientSecretKey; // 数据库对应的 Client Secret 配置键
    private final String redirectUriKey;  // 数据库对应的 回调地址 配置键
    private final String enabledKey;      // 数据库对应的 启用开关 配置键

    SocialPlatformEnum(String code, String name, String clientIdKey, String clientSecretKey, String redirectUriKey, String enabledKey) {
        this.code = code;
        this.name = name;
        this.clientIdKey = clientIdKey;
        this.clientSecretKey = clientSecretKey;
        this.redirectUriKey = redirectUriKey;
        this.enabledKey = enabledKey;
    }

    public static SocialPlatformEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (SocialPlatformEnum platform : values()) {
            if (platform.getCode().equalsIgnoreCase(code)) {
                return platform;
            }
        }
        return null;
    }
}
