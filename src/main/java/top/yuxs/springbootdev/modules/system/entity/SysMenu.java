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
 * 系统菜单与按钮权限表 (前端路由与页面元素控制)
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
@TableComment("系统菜单权限表")
@Index(name = "idx_sys_menu_parent_id", columns = {"parent_id"})
public class SysMenu extends BaseEntity {

    /**
     * 父菜单ID
     */
    @TableField("parent_id")
    @ColumnComment("父菜单ID")
    private Long parentId;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    @ColumnComment("菜单/按钮名称")
    private String menuName;

    /**
     * 前端跳转路由路径
     */
    @TableField("path")
    @ColumnComment("前端跳转路由路径")
    private String path;

    /**
     * 前端组件地址
     */
    @TableField("component")
    @ColumnComment("前端组件地址")
    private String component;

    /**
     * 按钮权限标识 (如 system:user:add)
     */
    @TableField("perms")
    @ColumnComment("按钮级权限标识")
    private String perms;

    /**
     * 菜单类型 (M: 目录, C: 菜单, F: 按钮)
     */
    @TableField("menu_type")
    @ColumnComment("菜单类型 (M: 目录, C: 菜单, F: 按钮)")
    private String menuType;

    /**
     * 菜单状态 (0: 正常, 1: 禁用)
     */
    @TableField("status")
    @ColumnComment("菜单状态 (0: 正常, 1: 禁用)")
    private Integer status;
}
