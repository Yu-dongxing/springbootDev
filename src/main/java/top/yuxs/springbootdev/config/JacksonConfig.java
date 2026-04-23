/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * Jackson 全局配置
 * 解决 前端 JavaScript 处理 Long 型数据精度丢失（截断）的问题
 * 仅在配置 jackson.long-to-string=true 时生效
 */
@Configuration
@ConditionalOnProperty(prefix = "jackson", name = "long-to-string", havingValue = "true", matchIfMissing = false)
public class JacksonConfig {

    @Bean
    public Module jacksonModule() {
        SimpleModule module = new SimpleModule();
        // 将 Long 和 BigInteger 类型在序列化时自动转为 String 类型
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        return module;
    }
}
