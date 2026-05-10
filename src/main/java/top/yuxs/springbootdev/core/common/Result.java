/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.common;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.yuxs.springbootdev.core.enums.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    // 成功响应
    public static <E> Result<E> success(E data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static Result<?> success() {
        return success(null);
    }

    // 失败响应 - 使用枚举
    public static <E> Result<E> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // 失败响应 - 枚举 + 自定义消息
    public static <E> Result<E> error(ResultCode resultCode, String customMessage) {
        return new Result<>(resultCode.getCode(), customMessage, null);
    }

    // 失败响应 - 枚举 + 详细数据 (用于存放 Exception Detail)
    public static <E> Result<E> error(ResultCode resultCode, E data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    // 兼容原有的 code, message 模式
    public static Result<?> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // 兼容带详细错误信息的模式
    public static <E> Result<E> error(int code, String message, E data) {
        return new Result<>(code, message, data);
    }
}