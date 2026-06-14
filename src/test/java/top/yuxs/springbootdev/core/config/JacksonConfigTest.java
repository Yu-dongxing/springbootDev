/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/14
 */

package top.yuxs.springbootdev.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonConfigTest {

    private ObjectMapper objectMapper;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestBean {
        private Long largeId;
        private Long smallNum;
        private Long negativeLargeId;
    }

    @BeforeEach
    void setUp() {
        // 手动构造与 JacksonConfig 等价的 ObjectMapper 实例进行纯净的单元测试
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, SmartLongSerializer.instance);
        module.addSerializer(Long.TYPE, SmartLongSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        objectMapper.registerModule(module);
    }

    @Test
    void testSmartLongSerializer() throws Exception {
        // 大于 JavaScript 最大安全整数 (9007199254740991)
        Long largeId = 180123456789012345L;
        // 小于 JavaScript 最大安全整数
        Long smallNum = 100L;
        // 小于 JavaScript 最小安全整数 (-9007199254740991)
        Long negativeLargeId = -180123456789012345L;

        TestBean bean = new TestBean(largeId, smallNum, negativeLargeId);
        String json = objectMapper.writeValueAsString(bean);

        System.out.println("Generated JSON: " + json);

        // 验证大整数被智能转为 String
        assertTrue(json.contains("\"largeId\":\"180123456789012345\""));
        // 验证小整数依然保留为 Number (不带双引号)
        assertTrue(json.contains("\"smallNum\":100"));
        // 验证负的大整数也被智能转为 String
        assertTrue(json.contains("\"negativeLargeId\":\"-180123456789012345\""));
    }
}
