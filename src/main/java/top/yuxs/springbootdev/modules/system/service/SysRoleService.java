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
}
