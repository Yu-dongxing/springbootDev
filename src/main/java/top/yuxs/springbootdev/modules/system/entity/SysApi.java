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
 * 物理 API 接口资源表 (后端动态路由安全保护)
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_api")
@TableComment("系统物理接口资源表")
@Index(name = "idx_sys_api_path_method", columns = {"path", "method"})
public class SysApi extends BaseEntity {

    /**
     * 接口友好描述/名称
     */
    @TableField("api_name")
    @ColumnComment("接口名称/说明描述")
    private String apiName;

    /**
     * 路由路径 (支持 Ant 通配符，如 /api/admin/sys-user/*)
     */
    @TableField("path")
    @ColumnComment("接口URL路由路径 (支持Ant风格通配符)")
    private String path;

    /**
     * 请求方式 (GET/POST/PUT/DELETE/*)
     */
    @TableField("method")
    @ColumnComment("HTTP请求方式 (GET/POST/PUT/DELETE/*)")
    private String method;

    /**
     * 所属业务模块
     */
    @TableField("module")
    @ColumnComment("所属业务模块")
    private String module;

    /**
     * 启用状态 (0: 启用, 1: 禁用)
     */
    @TableField("status")
    @ColumnComment("接口启用状态 (0: 启用, 1: 禁用)")
    private Integer status;
}
