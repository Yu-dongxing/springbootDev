/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.core.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.yuxs.springbootdev.modules.system.service.SysMenuService;
import top.yuxs.springbootdev.modules.system.service.SysRoleService;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义 Sa-Token 权限与角色数据加载源
 * 用于支持前端页面上的按钮级权限、角色过滤
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysMenuService sysMenuService;

    /**
     * 返回一个账号所拥有的按钮级权限标识集合 (前端用于 v-permission 按钮显隐过滤)
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = new ArrayList<>();
        
        // 1. 区分双端多账号体系鉴权：
        // 管理端：StpUtil 对应的 loginType 为 "login" (B端管理员)
        if ("login".equals(loginType)) {
            Long userId = Long.valueOf(loginId.toString());
            
            // 超级管理员赋予全局绝对特权
            if (sysRoleService.isSuperAdmin(userId)) {
                permissions.add("*");
                return permissions;
            }
            
            // 普通管理员：多表联查获取分配的 sys_menu 中的 perms
            return sysMenuService.getPermsByUserId(userId);
        }
        
        // C端普通用户端：StpUserUtil 对应的 loginType 为 "user"
        if ("user".equals(loginType)) {
            // 目前普通用户采取简单登录可用模式，如有特权，可在此处加载
            return permissions;
        }

        return permissions;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (前端、后端校验角色时自动调用)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        
        if ("login".equals(loginType)) {
            Long userId = Long.valueOf(loginId.toString());
            return sysRoleService.getRoleKeysByUserId(userId);
        }
        
        return roles;
    }
}
