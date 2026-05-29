/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.core.enums;

import lombok.Getter;

/**
 * 业务操作类型
 *
 * @author YuDongXing
 * @since 2026/05/28
 */
@Getter
public enum BusinessType {
    /**
     * 其它
     */
    OTHER("OTHER", "其它"),

    /**
     * 新增
     */
    INSERT("INSERT", "新增"),

    /**
     * 修改
     */
    UPDATE("UPDATE", "修改"),

    /**
     * 删除
     */
    DELETE("DELETE", "删除"),

    /**
     * 查询
     */
    SELECT("SELECT", "查询");

    private final String code;
    private final String message;

    BusinessType(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
