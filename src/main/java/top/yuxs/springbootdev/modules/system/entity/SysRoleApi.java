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
 * 角色与物理接口关系表
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_api")
@TableComment("角色与接口资源关联表")
@Index(name = "idx_role_api_rid", columns = {"role_id"})
@Index(name = "idx_role_api_aid", columns = {"api_id"})
public class SysRoleApi extends BaseEntity {

    @TableField("role_id")
    @ColumnComment("角色ID")
    private Long roleId;

    @TableField("api_id")
    @ColumnComment("物理API资源ID")
    private Long apiId;
}
