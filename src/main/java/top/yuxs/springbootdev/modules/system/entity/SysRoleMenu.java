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
 * 角色与菜单关系表
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
@TableComment("角色与菜单关联表")
@Index(name = "idx_role_menu_rid", columns = {"role_id"})
@Index(name = "idx_role_menu_mid", columns = {"menu_id"})
public class SysRoleMenu extends BaseEntity {

    @TableField("role_id")
    @ColumnComment("角色ID")
    private Long roleId;

    @TableField("menu_id")
    @ColumnComment("菜单ID")
    private Long menuId;
}
