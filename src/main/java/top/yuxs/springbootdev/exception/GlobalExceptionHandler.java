/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.exception;


import cn.dev33.satoken.exception.FirewallCheckException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenException;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import top.yuxs.springbootdev.common.*;
import top.yuxs.springbootdev.enums.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 不支持的请求方法 (GET/POST 错用)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.error("不支持的请求方法: {}, 错误信息={}", e.getMessage(), e);
        return Result.error(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 请求参数类型转换失败
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("参数 [%s] 类型错误", ex.getName());
        log.warn("请求参数类型错误: {}", message);
        return Result.error(ResultCode.PARAM_TYPE_BIND_ERROR, message);
    }

    /**
     * 统一处理参数校验异常 (JSR-303)
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String msg = ResultCode.PARAM_IS_INVALID.getMessage();
        FieldError fieldError = e.getFieldError();
        if (fieldError != null) {
            msg = fieldError.getField() + " " + fieldError.getDefaultMessage();
        }
        log.warn("参数校验异常: {}", msg);
        return Result.error(ResultCode.PARAM_IS_INVALID, msg);
    }

    /**
     * 必需参数缺失
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String msg = "必需的请求参数 '" + e.getParameterName() + "' 不存在";
        return Result.error(ResultCode.PARAM_NOT_COMPLETE, msg);
    }

    /**
     * JSON 解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体JSON解析失败: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_TYPE_BIND_ERROR, "请求参数格式错误（JSON格式不正确）");
    }

    /**
     * 数据类型转换错误 (NumberFormatException)
     */
    @ExceptionHandler(NumberFormatException.class)
    public Result<?> handNumberFormatException(NumberFormatException e){
        log.warn("数字转换错误: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_TYPE_BIND_ERROR, "数字格式错误");
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handNullPointerException(NullPointerException e){
        log.error("程序出现空指针异常: ", e);
        return Result.error(ResultCode.SYSTEM_INNER_ERROR, "系统内部参数空指针");
    }

    /**
     * SaToken 鉴权异常细分
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<?> handleSaTokenException(SaTokenException e) {
        int code = e.getCode();
        log.warn("SaToken异常: code={}, msg={}", code, e.getMessage());
        switch (code) {
            case 11001: case 11011: return Result.error(ResultCode.TOKEN_MISSING);
            case 11012: return Result.error(ResultCode.TOKEN_INVALID);
            case 11013: return Result.error(ResultCode.TOKEN_EXPIRED);
            case 11014: return Result.error(ResultCode.TOKEN_BE_REPLACED);
            case 11015: return Result.error(ResultCode.TOKEN_KICK_OUT);
            case 11016: return Result.error(ResultCode.TOKEN_FREEZE);
            case 11041: return Result.error(ResultCode.NO_PERMISSION);
            case 11042: return Result.error(ResultCode.NO_ROLE, "无此角色权限: " + e.getMessage());
            default: return Result.error(ResultCode.USER_NOT_LOGGED_IN, "认证失败");
        }
    }

    /**
     * SaToken 未登录异常补充
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLoginException(NotLoginException nle) {
        String type = nle.getType();
        if (NotLoginException.NOT_TOKEN.equals(type)) {
            return Result.error(ResultCode.TOKEN_MISSING);
        } else if (NotLoginException.INVALID_TOKEN.equals(type)) {
            return Result.error(ResultCode.TOKEN_INVALID);
        } else if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            return Result.error(ResultCode.TOKEN_EXPIRED);
        } else if (NotLoginException.BE_REPLACED.equals(type)) {
            return Result.error(ResultCode.TOKEN_BE_REPLACED);
        }
        return Result.error(ResultCode.USER_NOT_LOGGED_IN);
    }

    /**
     * 防火墙校验异常 (Sa-Token Firewall)
     */
    @ExceptionHandler(FirewallCheckException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleFirewallCheckException(FirewallCheckException e) {
        log.error("防火墙拦截: {}", e.getMessage());
        return Result.error(ResultCode.FORBIDDEN.getCode(), "请求被防火墙拦截: " + e.getMessage());
    }

    /**
     * 业务自定义异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(ResultCode.BUSINESS_ERROR, e.getMessage());
    }


    /**
     * 404 资源未找到
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e) {
        return Result.error(ResultCode.RESOURCE_NOT_FOUND);
    }

    /**
     * MyBatis / 数据库异常
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public Result<?> handleMyBatisSystemException(MyBatisSystemException e){
        log.error("数据库异常: ", e);
        return Result.error(ResultCode.DATABASE_ERROR);
    }

    /**
     * 全局兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleAllException(Exception e) {
        log.error("系统未知致命异常: ", e);
        return Result.error(ResultCode.SYSTEM_INNER_ERROR);
    }

}
