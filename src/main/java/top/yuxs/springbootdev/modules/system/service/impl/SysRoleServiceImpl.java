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
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysUserRoleMapper;
import top.yuxs.springbootdev.modules.system.service.SysRoleService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色 服务实现类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleKeys = this.getRoleKeysByUserId(userId);
        return !CollectionUtils.isEmpty(roleKeys) && roleKeys.contains("super_admin");
    }

    @Override
    public List<String> getRoleKeysByUserId(Long userId) {
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

        // Step 2: 根据角色 ID 集合查找可用的角色记录并抽取角色标识 (role_key)
        List<SysRole> roles = this.list(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 0)
        );
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }

        return roles.stream()
                .map(SysRole::getRoleKey)
                .distinct()
                .collect(Collectors.toList());
    }
}
