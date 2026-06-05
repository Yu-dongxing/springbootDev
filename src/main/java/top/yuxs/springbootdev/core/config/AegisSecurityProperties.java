/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/05
 */

package top.yuxs.springbootdev.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 神盾核心安全控制配置属性类
 *
 * @author YuDongXing
 * @since 2026/06/05
 */
@Data
@Component
@ConfigurationProperties(prefix = "aegis.security.auth")
public class AegisSecurityProperties {

    /**
     * 是否启用前端密码传输非对称加密 (默认: true)
     * 若开启，前端需拉取 RSA 公钥对明文进行加密，后端利用私钥解密还原后再操作。
     * 若关闭，接口直接将传入的 password 参数视为明文处理。
     */
    private boolean frontendEncryptEnabled = true;

    /**
     * 是否启用存储密码加密（BCrypt 散列哈希） (默认: true)
     * 若开启，用户注册和登录判定时均会通过高强度的加盐 BCrypt 散列密文对比。
     * 若关闭，直接在数据库存储明文密码，并用等值 equals 匹配。
     */
    private boolean passwordEncryptEnabled = true;
}
