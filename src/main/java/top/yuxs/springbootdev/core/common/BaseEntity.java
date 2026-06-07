/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.common;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import top.yuxs.springbootdev.core.db.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity implements Serializable {

    /**
     * 主键id(雪花算法id)
     */
    @ColumnComment("主键ID")
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 插入时填充
    @ColumnComment("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @ColumnComment("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}