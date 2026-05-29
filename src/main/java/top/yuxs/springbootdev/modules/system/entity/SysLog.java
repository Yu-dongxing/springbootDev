/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.yuxs.springbootdev.core.common.BaseEntity;
import top.yuxs.springbootdev.core.db.annotation.*;

import java.time.LocalDateTime;

/**
 * 系统操作日志表
 *
 * @author YuDongXing
 * @since 2026/05/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
@TableComment("系统操作日志表")
@Index(name = "idx_sys_log_user_id", columns = {"user_id"})
@Index(name = "idx_sys_log_create_time", columns = {"create_time"})
public class SysLog extends BaseEntity {

    /**
     * 操作用户ID
     */
    @TableField("user_id")
    @ColumnComment("操作用户ID")
    private Long userId;

    /**
     * 操作用户名
     */
    @TableField("username")
    @ColumnComment("操作用户名")
    private String username;

    /**
     * 用户角色列表
     */
    @TableField("user_role")
    @ColumnComment("用户角色列表")
    private String userRole;

    /**
     * 访问IP地址
     */
    @TableField("ip")
    @ColumnComment("访问IP地址")
    private String ip;

    /**
     * 请求URL路径
     */
    @TableField("url")
    @ColumnComment("请求URL路径")
    private String url;

    /**
     * 请求方式 (GET/POST/PUT/DELETE)
     */
    @TableField("method")
    @ColumnComment("请求方式 (GET/POST/PUT/DELETE)")
    private String method;

    /**
     * 执行类名
     */
    @TableField("class_name")
    @ColumnComment("执行类名")
    private String className;

    /**
     * 执行方法名
     */
    @TableField("method_name")
    @ColumnComment("执行方法名")
    private String methodName;

    /**
     * 接口注释/名称
     */
    @TableField("title")
    @ColumnComment("接口注释/名称")
    private String title;

    /**
     * 业务操作类型 (INSERT/UPDATE/DELETE/SELECT/OTHER)
     */
    @TableField("business_type")
    @ColumnComment("业务操作类型 (INSERT/UPDATE/DELETE/SELECT/OTHER)")
    private String businessType;

    /**
     * 完整请求入参
     */
    @TableField("param")
    @ColumnType("text")
    @ColumnComment("完整请求入参")
    private String param;

    /**
     * 返回出参/结果
     */
    @TableField("result")
    @ColumnType("text")
    @ColumnComment("返回出参/结果")
    private String result;

    /**
     * 执行状态 (1: 成功, 0: 失败)
     */
    @TableField("status")
    @ColumnComment("执行状态 (1: 成功, 0: 失败)")
    private Integer status;

    /**
     * 详细错误堆栈
     */
    @TableField("error_msg")
    @ColumnType("text")
    @ColumnComment("详细错误堆栈")
    private String errorMsg;

    /**
     * 执行耗时 (ms)
     */
    @TableField("take_time")
    @ColumnComment("执行耗时 (ms)")
    private Long takeTime;

    /**
     * 请求时间
     */
    @TableField("request_time")
    @ColumnComment("请求时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;
}
