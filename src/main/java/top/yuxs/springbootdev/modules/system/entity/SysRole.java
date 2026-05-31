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
 * 系统角色表
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@TableComment("系统角色表")
@Index(name = "idx_sys_role_key", columns = {"role_key"}, type = IndexType.UNIQUE)
public class SysRole extends BaseEntity {

    /**
     * 角色名称
     */
    @TableField("role_name")
    @ColumnComment("角色名称")
    private String roleName;

    /**
     * 角色标识 (如 super_admin)
     */
    @TableField("role_key")
    @ColumnComment("角色唯一标识")
    private String roleKey;

    /**
     * 角色状态 (0: 正常, 1: 禁用)
     */
    @TableField("status")
    @ColumnComment("角色状态 (0: 正常, 1: 禁用)")
    private Integer status;
}
