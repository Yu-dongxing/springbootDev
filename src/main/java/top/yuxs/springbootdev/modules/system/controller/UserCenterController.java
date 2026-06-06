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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.config.AegisSecurityProperties;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.core.utils.StpUserUtil;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.mapper.SysUserRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
import top.yuxs.springbootdev.modules.system.service.SysUserService;
import top.yuxs.springbootdev.modules.system.service.SysApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysApiService sysApiService;

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

    /**
     * 3. C端普通用户：获取个人中心完整资料 (含基本信息、关联角色key、API拦截权限清单)
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getMyProfile() {
        Long loginUserId = StpUserUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(loginUserId);
        if (user == null) {
            return Result.error(ResultCode.ERROR, "会话无效，当前登录用户不存在");
        }

        UserProfileVO profile = new UserProfileVO();

        // 1. 装载用户信息 (VO 隔离敏感数据)
        SysUserVO uVo = new SysUserVO();
        uVo.setId(user.getId());
        uVo.setUsername(user.getUsername());
        uVo.setUserType(user.getUserType());
        uVo.setStatus(user.getStatus());
        uVo.setCreateTime(user.getCreateTime());
        uVo.setUpdateTime(user.getUpdateTime());
        profile.setUserInfo(uVo);

        // 2. 查出 C 端用户的角色与权限
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, loginUserId)
        );
        List<String> roleKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userRoles)) {
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            List<SysRole> roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)
            );
            roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList());
        }
        profile.setRoles(roleKeys);

        Set<String> apiPerms = sysApiService.getApiPermissionsByUserId(loginUserId);
        profile.setPermissions(new ArrayList<>(apiPerms));

        return Result.success(profile);
    }

    @Data
    public static class UserProfileVO {
        private SysUserVO userInfo;
        private List<String> roles;
        private List<String> permissions;
    }

    @Data
    public static class SysUserVO {
        private Long id;
        private String username;
        private String userType;
        private Integer status;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
    }

    @Data
    public static class UpdatePasswordParam {
        private String oldPassword;
        private String newPassword;
    }
}
