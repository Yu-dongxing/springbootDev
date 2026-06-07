/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.yuxs.springbootdev.modules.system.entity.SysRole;

import java.util.List;

/**
 * 系统角色 服务类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 判断管理员用户是否为“超级管理员” (判断其拥有的 roleKey 是否包含 "super_admin")
     */
    boolean isSuperAdmin(Long userId);

    /**
     * 根据管理员ID查询拥有的可用角色键
     */
    List<String> getRoleKeysByUserId(Long userId);

    /**
     * 为角色分配物理 API 接口权限
     * @param roleId 角色ID
     * @param apiIds API ID 集合
     */
    void assignApisToRole(Long roleId, List<Long> apiIds);

    /**
     * 为用户分配系统角色
     * @param userId 用户ID
     * @param roleIds 角色ID 集合
     */
    void assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 删除系统角色 (级联清除 sys_role_api, sys_user_role 关系, 并清空受影响用户缓存)
     * @param roleId 角色ID
     */
    void deleteRole(Long roleId);

    /**
     * 根据角色ID获取已绑定的物理 API 接口 ID 列表
     * @param roleId 角色ID
     * @return API ID 列表
     */
    List<Long> getApiIdsByRoleId(Long roleId);

    /**
     * 级联更新角色信息 (并同步极速清理属于该角色的关联用户的网关权限缓存，保证权限实时自愈生效)
     * @param sysRole 待更新角色实体
     * @return 是否更新成功
     */
    boolean updateRole(SysRole sysRole);
}
