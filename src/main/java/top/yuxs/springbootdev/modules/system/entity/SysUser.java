/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.yuxs.springbootdev.core.common.BaseEntity;
import top.yuxs.springbootdev.core.db.annotation.*;
import top.yuxs.springbootdev.core.enums.db.IndexType;

/**
 * 系统统一用户表 (双端多账号体系)
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@TableComment("系统统一用户表")
@Index(name = "idx_sys_user_username", columns = {"username"}, type = IndexType.UNIQUE)
@Index(name = "idx_sys_user_type", columns = {"user_type"})
public class SysUser extends BaseEntity {

    /**
     * 登录账号
     */
    @TableField("username")
    @ColumnComment("登录账号")
    private String username;

    /**
     * 加密密码
     */
    @TableField("password")
    @ColumnComment("加密密码")
    private String password;

    /**
     * 账户类型 (ADMIN: 管理员, USER: 普通用户)
     */
    @TableField("user_type")
    @ColumnComment("账户类型 (ADMIN: 管理端, USER: 普通用户)")
    private String userType;

    /**
     * 账号状态 (0: 正常, 1: 禁用)
     */
    @TableField("status")
    @ColumnComment("账号状态 (0: 正常, 1: 禁用)")
    private Integer status;
}
