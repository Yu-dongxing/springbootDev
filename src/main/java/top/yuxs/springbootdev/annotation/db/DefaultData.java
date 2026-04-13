/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/13
 */

package top.yuxs.springbootdev.annotation.db;

import java.lang.annotation.*;

/**
 * 标注字段的初始默认数据
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DefaultData {
    /**
     * 初始值（字符串形式，程序会自动转换类型）
     */
    String value();
}
