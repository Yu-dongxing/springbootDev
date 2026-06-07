/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import top.yuxs.springbootdev.modules.system.entity.SysMenu;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.entity.SysRoleMenu;
import top.yuxs.springbootdev.modules.system.mapper.SysMenuMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysUserRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMenuMapper;
import top.yuxs.springbootdev.modules.system.service.SysMenuService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统菜单权限 服务实现类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public List<String> getPermsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }

        // Step 1: 根据 userId 查找拥有的角色 ID 集合
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );
        if (CollectionUtils.isEmpty(userRoles)) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        // 过滤掉被禁用的角色（status != 0 的被过滤，只保留 status == 0 的正常角色）
        List<SysRole> activeRoles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 0)
        );
        if (CollectionUtils.isEmpty(activeRoles)) {
            return List.of();
        }
        List<Long> activeRoleIds = activeRoles.stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());

        // Step 2: 根据活跃角色 ID 集合查找关联的菜单 ID 集合
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, activeRoleIds)
        );
        if (CollectionUtils.isEmpty(roleMenus)) {
            return List.of();
        }
        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // Step 3: 根据菜单 ID 集合查找可用且有权限标识的菜单记录
        List<SysMenu> menus = this.list(
                new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, menuIds)
                        .eq(SysMenu::getStatus, 0)
                        .isNotNull(SysMenu::getPerms)
                        .ne(SysMenu::getPerms, "")
        );
        if (CollectionUtils.isEmpty(menus)) {
            return List.of();
        }

        return menus.stream()
                .map(SysMenu::getPerms)
                .distinct()
                .collect(Collectors.toList());
    }
}
