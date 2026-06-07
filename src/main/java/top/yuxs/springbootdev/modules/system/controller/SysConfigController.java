/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/07
 */

package top.yuxs.springbootdev.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.modules.system.entity.SysConfig;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;

import java.util.Arrays;
import java.util.List;

/**
 * 系统配置参数管理控制器 (基于 Aegis 动态扫描机制)
 *
 * @author YuDongXing
 * @since 2026/06/07
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sys-config")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    // 内置核心系统配置，禁止删除或禁用，防止核心安全链路崩塌
    private static final List<String> PROTECTED_KEYS = Arrays.asList(
            "sys.auth.rsa.public-key",
            "sys.auth.rsa.private-key"
    );

    /**
     * 1. 分页查询系统配置列表
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) Integer status) {

        Page<SysConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(configKey)) {
            wrapper.like(SysConfig::getConfigKey, configKey.trim());
        }
        if (StringUtils.hasText(configName)) {
            wrapper.like(SysConfig::getConfigName, configName.trim());
        }
        if (status != null) {
            wrapper.eq(SysConfig::getStatus, status);
        }
        wrapper.orderByDesc(SysConfig::getCreateTime);

        return Result.success(sysConfigService.page(page, wrapper));
    }

    /**
     * 2. 创建新系统配置
     */
    @PostMapping("/create")
    public Result<?> create(@RequestBody SysConfig sysConfig) {
        if (sysConfig == null || !StringUtils.hasText(sysConfig.getConfigKey()) || !StringUtils.hasText(sysConfig.getConfigValue())) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "配置键名和配置值不能为空");
        }

        // 校验唯一性
        SysConfig exist = sysConfigService.getOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, sysConfig.getConfigKey().trim())
        );
        if (exist != null) {
            return Result.error(ResultCode.ERROR, "配置键名已存在，不能重复创建！");
        }

        sysConfig.setConfigKey(sysConfig.getConfigKey().trim());
        sysConfig.setConfigValue(sysConfig.getConfigValue().trim());
        if (StringUtils.hasText(sysConfig.getConfigName())) {
            sysConfig.setConfigName(sysConfig.getConfigName().trim());
        }
        if (StringUtils.hasText(sysConfig.getRemark())) {
            sysConfig.setRemark(sysConfig.getRemark().trim());
        }
        if (sysConfig.getStatus() == null) {
            sysConfig.setStatus(0); // 默认正常启用
        }

        sysConfigService.save(sysConfig);
        return Result.success("系统配置参数创建成功");
    }

    /**
     * 3. 编辑修改系统配置信息
     */
    @PutMapping("/update")
    public Result<?> update(@RequestBody SysConfig sysConfig) {
        if (sysConfig == null || sysConfig.getId() == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "配置ID不能为空");
        }

        SysConfig exist = sysConfigService.getById(sysConfig.getId());
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "待更新的配置不存在");
        }

        // 物理盾校验：如果是受保护的核心 Key，禁止改写其键名 (防止把核心键名称改掉导致系统崩塌)
        if (PROTECTED_KEYS.contains(exist.getConfigKey())) {
            if (sysConfig.getConfigKey() != null && !sysConfig.getConfigKey().equals(exist.getConfigKey())) {
                return Result.error(ResultCode.ERROR, "安全物理防线：禁止修改核心系统参数的 Key 标识键名！");
            }
            if (sysConfig.getStatus() != null && sysConfig.getStatus() == 1) {
                return Result.error(ResultCode.ERROR, "安全物理防线：内置核心传输安全防线配置，禁止禁用！");
            }
        }

        // 修改 configKey 时查重
        if (StringUtils.hasText(sysConfig.getConfigKey())) {
            String newKey = sysConfig.getConfigKey().trim();
            SysConfig keyConflict = sysConfigService.getOne(
                    new LambdaQueryWrapper<SysConfig>()
                            .eq(SysConfig::getConfigKey, newKey)
                            .ne(SysConfig::getId, sysConfig.getId())
            );
            if (keyConflict != null) {
                return Result.error(ResultCode.ERROR, "修改后的配置键名已存在，不能重复！");
            }
            exist.setConfigKey(newKey);
        }

        if (sysConfig.getConfigValue() != null) {
            exist.setConfigValue(sysConfig.getConfigValue());
        }
        if (sysConfig.getConfigName() != null) {
            exist.setConfigName(sysConfig.getConfigName().trim());
        }
        if (sysConfig.getRemark() != null) {
            exist.setRemark(sysConfig.getRemark().trim());
        }
        if (sysConfig.getStatus() != null) {
            exist.setStatus(sysConfig.getStatus());
        }

        sysConfigService.updateById(exist);
        return Result.success("系统配置参数更新成功");
    }

    /**
     * 4. 物理删除系统配置
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "参数ID不能为空");
        }

        SysConfig exist = sysConfigService.getById(id);
        if (exist == null) {
            return Result.success("配置已被删除或不存在");
        }

        // 物理盾校验：内置核心配置拒绝物理删除
        if (PROTECTED_KEYS.contains(exist.getConfigKey())) {
            return Result.error(ResultCode.ERROR, "安全防御限制：核心非对称流 RSA 密钥等参数为系统底层运转所必须，禁止物理删除！");
        }

        sysConfigService.removeById(id);
        return Result.success("系统配置参数删除成功");
    }
}
