/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/06
 */

package top.yuxs.springbootdev.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.service.SysRoleService;

import java.util.List;

/**
 * 系统角色管理控制器
 *
 * @author YuDongXing
 * @since 2026/06/06
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sys-role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 1. 分页查询系统角色列表
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) Integer status) {
        
        Page<SysRole> page = new Page<>(current, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(roleName)) {
            wrapper.like(SysRole::getRoleName, roleName.trim());
        }
        if (StringUtils.hasText(roleKey)) {
            wrapper.like(SysRole::getRoleKey, roleKey.trim());
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByDesc(SysRole::getCreateTime);
        
        return Result.success(sysRoleService.page(page, wrapper));
    }

    /**
     * 2. 创建新系统角色
     */
    @PostMapping("/create")
    public Result<?> create(@RequestBody SysRole sysRole) {
        if (sysRole == null || !StringUtils.hasText(sysRole.getRoleKey()) || !StringUtils.hasText(sysRole.getRoleName())) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "角色名称和唯一标识Key不能为空");
        }
        
        // 校验唯一性
        SysRole exist = sysRoleService.getOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, sysRole.getRoleKey().trim())
        );
        if (exist != null) {
            return Result.error(ResultCode.ERROR, "角色唯一标识Key已存在，不能重复创建！");
        }
        
        sysRole.setRoleKey(sysRole.getRoleKey().trim());
        sysRole.setRoleName(sysRole.getRoleName().trim());
        if (sysRole.getStatus() == null) {
            sysRole.setStatus(0); // 默认正常启用
        }
        
        sysRoleService.save(sysRole);
        return Result.success("系统角色创建成功");
    }

    /**
     * 3. 编辑修改系统角色信息
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody SysRole sysRole) {
        if (sysRole == null || sysRole.getId() == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "角色ID不能为空");
        }
        
        SysRole exist = sysRoleService.getById(sysRole.getId());
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "待更新的系统角色不存在");
        }
        
        // 禁止修改内置角色的 Key (防止修改超级管理员和系统管理员的 key 破坏自愈初始化和安全防守)
        if ("super_admin".equals(exist.getRoleKey()) || "admin".equals(exist.getRoleKey())) {
            if (sysRole.getRoleKey() != null && !sysRole.getRoleKey().equals(exist.getRoleKey())) {
                return Result.error(ResultCode.ERROR, "安全防御：禁止修改内置角色的唯一 Key 标识");
            }
        }
        
        if (StringUtils.hasText(sysRole.getRoleName())) {
            exist.setRoleName(sysRole.getRoleName().trim());
        }
        if (StringUtils.hasText(sysRole.getRoleKey())) {
            // 查一下是否冲突
            SysRole keyConflict = sysRoleService.getOne(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleKey, sysRole.getRoleKey().trim())
                            .ne(SysRole::getId, sysRole.getId())
            );
            if (keyConflict != null) {
                return Result.error(ResultCode.ERROR, "修改后的唯一 Key 已被其他角色占用");
            }
            exist.setRoleKey(sysRole.getRoleKey().trim());
        }
        if (sysRole.getStatus() != null) {
            exist.setStatus(sysRole.getStatus());
        }
        
        sysRoleService.updateById(exist);
        return Result.success("角色信息更新成功");
    }

    /**
     * 4. 物理删除系统角色并级联清空关联
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "角色ID不能为空");
        }
        
        SysRole exist = sysRoleService.getById(id);
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "待删除角色不存在");
        }
        
        // 核心防卫：内置超级管理员和管理员角色绝对不容许删除
        if ("super_admin".equals(exist.getRoleKey()) || "admin".equals(exist.getRoleKey())) {
            return Result.error(ResultCode.ERROR, "系统防御：不能删除内置的基础管理角色(" + exist.getRoleKey() + ")");
        }
        
        sysRoleService.deleteRole(id);
        return Result.success("系统角色及级联绑定物理删除成功");
    }

    /**
     * 5. 为角色分配物理 API 接口权限
     */
    @PostMapping("/assign-apis")
    public Result<?> assignApis(@RequestBody AssignApisParam param) {
        if (param == null || param.getRoleId() == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "入参不完整，roleId 必填");
        }
        
        sysRoleService.assignApisToRole(param.getRoleId(), param.getApiIds());
        return Result.success("分配物理 API 接口权限成功，受影响管理员的鉴权缓存已实时重置。");
    }

    /**
     * 6. 分配系统角色给指定用户
     */
    @PostMapping("/assign-to-user")
    public Result<?> assignToUser(@RequestBody AssignRolesParam param) {
        if (param == null || param.getUserId() == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "入参不完整，userId 必填");
        }
        
        sysRoleService.assignRolesToUser(param.getUserId(), param.getRoleIds());
        return Result.success("分配系统角色成功，该用户的鉴权缓存已实时重置。");
    }

    @Data
    public static class AssignApisParam {
        private Long roleId;
        private List<Long> apiIds;
    }

    @Data
    public static class AssignRolesParam {
        private Long userId;
        private List<Long> roleIds;
    }
}
