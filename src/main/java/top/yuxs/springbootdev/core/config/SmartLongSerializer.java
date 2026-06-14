/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/14
 */

package top.yuxs.springbootdev.core.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 智能 Long 序列化器
 * 只有在 Long 的数值超出 JavaScript 的安全数值上限 (2^53 - 1) 或下限 -(2^53 - 1) 时才转为 String 传输，
 * 否则继续以原生的 Number 传输。
 * 
 * @author YuDongXing
 * @since 2026/06/14
 */
public class SmartLongSerializer extends JsonSerializer<Long> {

    // JavaScript 的最大安全整数上限：2^53 - 1
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    // JavaScript 的最小安全整数下限：-(2^53 - 1)
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;

    public static final SmartLongSerializer instance = new SmartLongSerializer();

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (value > MAX_SAFE_INTEGER || value < MIN_SAFE_INTEGER) {
            gen.writeString(value.toString());
        } else {
            gen.writeNumber(value);
        }
    }
}
