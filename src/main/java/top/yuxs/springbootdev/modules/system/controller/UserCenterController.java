/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/06
 */

package top.yuxs.springbootdev.modules.system.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.config.AegisSecurityProperties;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.core.utils.StpUserUtil;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.service.SysUserService;

/**
 * C 端普通用户个人中心自维护控制器 (物理隔离安全防守)
 *
 * @author YuDongXing
 * @since 2026/06/06
 */
@Slf4j
@RestController
@RequestMapping("/api/user/my")
public class UserCenterController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AegisSecurityProperties securityProperties;

    /**
     * 1. C端普通用户：修改自我基本信息
     */
    @PutMapping("/info")
    public Result<?> updateMyInfo(@RequestBody SysUser sysUser) {
        // 利用 StpUserUtil 获取当前 C端 登录会话的用户ID
        Long loginUserId = StpUserUtil.getLoginIdAsLong();
        
        SysUser exist = sysUserService.getById(loginUserId);
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "会话无效，当前登录用户不存在");
        }
        
        // 允许修改登录账号（用户名）并进行防重校验
        if (sysUser != null && StringUtils.hasText(sysUser.getUsername())) {
            String newUsername = sysUser.getUsername().trim();
            SysUser conflict = sysUserService.getOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, newUsername)
                            .ne(SysUser::getId, loginUserId)
            );
            if (conflict != null) {
                return Result.error(ResultCode.ERROR, "该用户名已被他人使用");
            }
            exist.setUsername(newUsername);
        }
        
        sysUserService.updateById(exist);
        return Result.success("修改个人信息成功");
    }

    /**
     * 2. C端普通用户：修改自我登录密码
     */
    @PostMapping("/password")
    public Result<?> updateMyPassword(@RequestBody UpdatePasswordParam param) {
        if (param == null || !StringUtils.hasText(param.getOldPassword()) || !StringUtils.hasText(param.getNewPassword())) {
            return Result.error(ResultCode.PARAM_IS_BLANK, "旧密码和新密码均不能为空");
        }
        
        Long loginUserId = StpUserUtil.getLoginIdAsLong();
        SysUser exist = sysUserService.getById(loginUserId);
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "会话无效，当前登录用户不存在");
        }
        
        // 校验旧密码
        String dbPassword = exist.getPassword();
        String oldPassword = param.getOldPassword();
        
        boolean match = false;
        if (securityProperties.isPasswordEncryptEnabled()) {
            match = BCrypt.checkpw(oldPassword, dbPassword);
        } else {
            match = oldPassword.equals(dbPassword);
        }
        
        if (!match) {
            return Result.error(ResultCode.ERROR, "旧密码输入不正确");
        }
        
        // 密码哈希落库自适应
        String newPassword = param.getNewPassword();
        if (securityProperties.isPasswordEncryptEnabled()) {
            exist.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        } else {
            exist.setPassword(newPassword);
        }
        
        sysUserService.updateById(exist);
        
        // 强制踢自己下线重登录
        StpUserUtil.logout();
        
        return Result.success("修改密码成功，请重新登录！");
    }

    @Data
    public static class UpdatePasswordParam {
        private String oldPassword;
        private String newPassword;
    }
}
