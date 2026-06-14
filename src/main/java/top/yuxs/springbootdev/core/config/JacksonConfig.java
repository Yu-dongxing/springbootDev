/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.config;

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
        // 使用更智能的 SmartLongSerializer，避免将所有小 Long 字段一刀切转为 String 破坏接口纯净性
        module.addSerializer(Long.class, SmartLongSerializer.instance);
        module.addSerializer(Long.TYPE, SmartLongSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        return module;
    }
}
