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

/**
 * 用户与角色关系表
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
@TableComment("用户与角色关联表")
@Index(name = "idx_user_role_uid", columns = {"user_id"})
@Index(name = "idx_user_role_rid", columns = {"role_id"})
public class SysUserRole extends BaseEntity {

    @TableField("user_id")
    @ColumnComment("用户ID")
    private Long userId;

    @TableField("role_id")
    @ColumnComment("角色ID")
    private Long roleId;
}
