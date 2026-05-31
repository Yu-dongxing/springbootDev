/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.core.utils;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Sa-Token C端普通用户端多账号隔离认证工具类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
public class StpUserUtil {

    /**
     * 账号类型标识，为 "user"，物理隔绝 B端管理端（StpUtil 默认的 "login" 类型）
     */
    public static final String TYPE = "user";

    /**
     * 底层的 StpLogic 实例
     */
    public static StpLogic stpLogic = new StpLogic(TYPE);

    /**
     * 会话登录
     */
    public static void login(Object id) {
        stpLogic.login(id);
    }

    /**
     * 会话注销
     */
    public static void logout() {
        stpLogic.logout();
    }

    /**
     * 当前会话是否登录
     */
    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Object getLoginId() {
        return stpLogic.getLoginId();
    }

    /**
     * 获取当前登录用户ID (转化为 long 类型)
     */
    public static long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }

    /**
     * 获取当前登录会话的 Token 值
     */
    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    /**
     * 强制校验当前会话是否登录，未登录则抛出异常
     */
    public static void checkLogin() {
        stpLogic.checkLogin();
    }
}
