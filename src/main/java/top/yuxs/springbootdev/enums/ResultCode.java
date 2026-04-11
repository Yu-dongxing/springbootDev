/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.enums;


import lombok.Getter;

@Getter
public enum ResultCode {
    /* 成功 ，失败状态码 */
    SUCCESS(200, "操作成功"),
    ERROR(2001,"操作失败"),

    /* 参数错误：4001 - 4999 */
    PARAM_IS_INVALID(4001, "参数无效"),
    PARAM_IS_BLANK(4002, "参数为空"),
    PARAM_TYPE_BIND_ERROR(4003, "参数类型错误"),
    PARAM_NOT_COMPLETE(4004, "参数缺失"),

    /* 用户认证/权限错误：1001 - 1999 */
    USER_NOT_LOGGED_IN(1001, "当前会话未登录"),
    TOKEN_MISSING(1002, "未能读取到有效Token"),
    TOKEN_INVALID(1003, "Token无效"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    TOKEN_BE_REPLACED(1005, "Token已被顶下线"),
    TOKEN_KICK_OUT(1006, "Token已被踢下线"),
    TOKEN_FREEZE(1007, "Token已被冻结"),
    NO_PERMISSION(1008, "无权限，请联系管理员"),
    NO_ROLE(1009, "无此角色权限"),

    /* 系统错误：5001 - 5999 */
    SYSTEM_INNER_ERROR(5001, "系统繁忙，未知错误，请稍后再试"),
    DATABASE_ERROR(5002, "数据库操作异常"),
    METHOD_NOT_ALLOWED(5003, "不支持的请求方法"),
    RESOURCE_NOT_FOUND(5004, "您访问的资源不存在"),

    /* 业务错误：6001 - 6999 */
    BUSINESS_ERROR(6001, "业务执行异常"),
    CONTRACT_ERROR(6002, "合约执行异常"),
    EXTERNAL_SERVICE_ERROR(6003, "外部服务调用异常");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}