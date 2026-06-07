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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.entity.SysRoleApi;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysUserRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleApiMapper;
import top.yuxs.springbootdev.modules.system.service.SysRoleService;
import top.yuxs.springbootdev.modules.system.service.SysApiService;

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

    @Autowired
    private SysRoleApiMapper sysRoleApiMapper;

    @Autowired
    private SysApiService sysApiService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignApisToRole(Long roleId, List<Long> apiIds) {
        if (roleId == null) {
            return;
        }
        // 1. 物理清旧绑定
        sysRoleApiMapper.delete(
                new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getRoleId, roleId)
        );

        // 2. 批量插入新绑定
        if (!CollectionUtils.isEmpty(apiIds)) {
            for (Long apiId : apiIds) {
                SysRoleApi sra = new SysRoleApi();
                sra.setRoleId(roleId);
                sra.setApiId(apiId);
                sysRoleApiMapper.insert(sra);
            }
        }

        // 3. 根据 roleId 联合查询所有被授权的用户，依次清空其 Redis 中的网关权限缓存
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId)
        );
        if (!CollectionUtils.isEmpty(userRoles)) {
            for (SysUserRole ur : userRoles) {
                sysApiService.clearUserApiCache(ur.getUserId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return;
        }
        // 1. 物理级联清空该用户的旧角色绑定
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );

        // 2. 保存新角色绑定
        if (!CollectionUtils.isEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }

        // 3. 极速清除该用户的 Redis 网关鉴权缓存
        sysApiService.clearUserApiCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        if (roleId == null) {
            return;
        }
        // 1. 联合查询绑定了此角色的所有用户，用于后续缓存清理
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId)
        );

        // 2. 物理删除角色记录
        this.removeById(roleId);

        // 3. 物理级联清除相关关联记录 sys_role_api, sys_user_role
        sysRoleApiMapper.delete(
                new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getRoleId, roleId)
        );
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId)
        );

        // 4. 清空受影响用户的 Redis 鉴权缓存
        if (!CollectionUtils.isEmpty(userRoles)) {
            for (SysUserRole ur : userRoles) {
                sysApiService.clearUserApiCache(ur.getUserId());
            }
        }
    }

    @Override
    public List<Long> getApiIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        List<SysRoleApi> list = sysRoleApiMapper.selectList(
                new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getRoleId, roleId)
        );
        return list.stream().map(SysRoleApi::getApiId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole sysRole) {
        if (sysRole == null || sysRole.getId() == null) {
            return false;
        }
        boolean success = this.updateById(sysRole);
        if (success) {
            // 极速清理所有属于该角色的用户的鉴权缓存，保障修改角色（如禁用、删除等）能够瞬间触发自愈
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, sysRole.getId())
            );
            if (!CollectionUtils.isEmpty(userRoles)) {
                for (SysUserRole ur : userRoles) {
                    sysApiService.clearUserApiCache(ur.getUserId());
                }
            }
        }
        return success;
    }
}
