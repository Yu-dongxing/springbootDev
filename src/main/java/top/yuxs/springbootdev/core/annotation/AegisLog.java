/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.core.annotation;

import top.yuxs.springbootdev.core.enums.BusinessType;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 *
 * @author YuDongXing
 * @since 2026/05/28
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AegisLog {
    /**
     * 模块/接口注释名称 (如："获取枚举列表")
     * 如果不填，系统将采用自动推导机制兜底
     */
    String title() default "";

    /**
     * 业务操作类型 (默认其它，如果不填，系统会根据 HTTP Method 与方法前缀自动推导)
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 是否保存请求的参数 (默认保存)
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应的返回数据 (默认保存)
     */
    boolean isSaveResponseData() default true;
}
