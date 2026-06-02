/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/01
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
 * 系统通用参数配置表 (基于 Code-First 自动维护结构)
 *
 * @author YuDongXing
 * @since 2026/06/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
@TableComment("系统通用参数配置表")
@Index(name = "idx_sys_config_key", columns = {"config_key"}, type = IndexType.UNIQUE)
public class SysConfig extends BaseEntity {

    /**
     * 配置键名 (唯一识别码)
     */
    @TableField("config_key")
    @ColumnComment("配置键名")
    private String configKey;

    /**
     * 配置键值
     */
    @TableField("config_value")
    @ColumnComment("配置键值")
    @ColumnType("text") // text 类型，以适配存储长 RSA 私钥、大 JSON 等
    private String configValue;

    /**
     * 配置名称 (友好名称)
     */
    @TableField("config_name")
    @ColumnComment("配置名称")
    private String configName;

    /**
     * 备注说明
     */
    @TableField("remark")
    @ColumnComment("配置备注说明")
    private String remark;

    /**
     * 启用状态 (0: 正常/启用, 1: 禁用)
     */
    @TableField("status")
    @ColumnComment("状态 (0: 正常/启用, 1: 禁用)")
    private Integer status;
}
