/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/06
 */

package top.yuxs.springbootdev.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.modules.system.entity.SysApi;
import top.yuxs.springbootdev.modules.system.entity.SysRoleApi;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleApiMapper;
import top.yuxs.springbootdev.modules.system.service.SysApiService;
import top.yuxs.springbootdev.modules.system.scanner.SysApiScanner;

/**
 * 物理 API 接口资源后台管理控制器
 *
 * @author YuDongXing
 * @since 2026/06/06
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sys-api")
public class SysApiController {

    @Autowired
    private SysApiService sysApiService;

    @Autowired
    private SysRoleApiMapper sysRoleApiMapper;

    @Autowired
    private SysApiScanner sysApiScanner;

    /**
     * 1. 物理 API 列表分页查询
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String apiName,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String module) {
        
        Page<SysApi> page = new Page<>(current, size);
        LambdaQueryWrapper<SysApi> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(apiName)) {
            wrapper.like(SysApi::getApiName, apiName.trim());
        }
        if (StringUtils.hasText(path)) {
            wrapper.like(SysApi::getPath, path.trim());
        }
        if (StringUtils.hasText(method)) {
            wrapper.eq(SysApi::getMethod, method.trim().toUpperCase());
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysApi::getModule, module.trim());
        }
        wrapper.orderByDesc(SysApi::getCreateTime);
        
        return Result.success(sysApiService.page(page, wrapper));
    }

    /**
     * 2. 编辑修改物理 API 的描述 or 状态
     */
    @PutMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(@RequestBody SysApi sysApi) {
        if (sysApi == null || sysApi.getId() == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "接口主键ID不能为空");
        }
        
        SysApi exist = sysApiService.getById(sysApi.getId());
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "待更新的物理接口不存在");
        }
        
        // 允许修改 apiName 和 status
        if (sysApi.getApiName() != null) {
            exist.setApiName(sysApi.getApiName());
        }
        if (sysApi.getStatus() != null) {
            exist.setStatus(sysApi.getStatus());
        }
        
        sysApiService.updateById(exist);
        return Result.success("修改接口信息成功");
    }

    /**
     * 3. 物理删除单个废弃 API 并级联清除 sys_role_api
     */
    @DeleteMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@PathVariable Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "物理接口主键ID不能为空");
        }
        
        SysApi exist = sysApiService.getById(id);
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "待删除的接口不存在");
        }
        
        // 物理删除接口
        sysApiService.removeById(id);
        
        // 级联物理清理绑定的角色关系 sys_role_api
        sysRoleApiMapper.delete(
                new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getApiId, id)
        );
        
        log.info(">>>>>> [管理端物理 API 清理] 手动物理删除废弃 API: {}, 并清空级联映射", exist.getPath());
        return Result.success("物理接口删除及角色级联解绑成功");
    }

    /**
     * 4. 手动主动一键触发代码全量物理 API 扫描注册与同步
     */
    @PostMapping("/sync-trigger")
    public Result<?> syncTrigger() {
        log.info(">>>>>> 用户手动一键触发物理 API 同步机制...");
        sysApiScanner.performScanAndSync();
        return Result.success("物理接口扫描与全自动赋权同步触发成功");
    }
}
