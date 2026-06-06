/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/06
 */

package top.yuxs.springbootdev.modules.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.config.AegisSecurityProperties;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.entity.SysUserSocial;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.mapper.SysUserRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysUserSocialMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
import top.yuxs.springbootdev.modules.system.service.SysUserService;
import top.yuxs.springbootdev.modules.system.service.SysApiService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 后台系统用户维护与管理员自维护控制器
 *
 * @author YuDongXing
 * @since 2026/06/06
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sys-user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysApiService sysApiService;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysUserSocialMapper sysUserSocialMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private AegisSecurityProperties securityProperties;

    /**
     * 1. 用户分页查询 (级联返回第三方绑定及角色列表)
     */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String userType) {
        
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUser::getUsername, username.trim());
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (StringUtils.hasText(userType)) {
            wrapper.eq(SysUser::getUserType, userType.trim().toUpperCase());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        sysUserService.page(page, wrapper);
        
        List<SysUser> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return Result.success(page);
        }
        
        List<Long> userIds = records.stream().map(SysUser::getId).collect(Collectors.toList());
        
        // 单表查询 Step A: 批量获取这些用户的社交账号绑定
        List<SysUserSocial> allSocials = sysUserSocialMapper.selectList(
                new LambdaQueryWrapper<SysUserSocial>().in(SysUserSocial::getUserId, userIds)
        );
        Map<Long, List<SysUserSocial>> socialMap = allSocials.stream()
                .collect(Collectors.groupingBy(SysUserSocial::getUserId));
        
        // 单表查询 Step B: 批量获取用户角色绑定
        List<SysUserRole> allUserRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds)
        );
        
        Map<Long, List<Long>> userRoleIdsMap = new HashMap<>();
        Set<Long> roleIds = new HashSet<>();
        for (SysUserRole ur : allUserRoles) {
            userRoleIdsMap.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(ur.getRoleId());
            roleIds.add(ur.getRoleId());
        }
        
        // 批量查询角色详情
        Map<Long, SysRole> roleMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)
            );
            roleMap = roles.stream().collect(Collectors.toMap(SysRole::getId, r -> r));
        }
        
        // 装配成 VO
        List<SysUserVO> voList = new ArrayList<>();
        for (SysUser user : records) {
            SysUserVO vo = new SysUserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setUserType(user.getUserType());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            vo.setUpdateTime(user.getUpdateTime());
            
            // 组装社交
            vo.setSocials(socialMap.getOrDefault(user.getId(), List.of()));
            
            // 组装角色
            List<Long> uRoleIds = userRoleIdsMap.getOrDefault(user.getId(), List.of());
            List<SysRole> uRoles = new ArrayList<>();
            for (Long rid : uRoleIds) {
                SysRole role = roleMap.get(rid);
                if (role != null) {
                    uRoles.add(role);
                }
            }
            vo.setRoles(uRoles);
            
            voList.add(vo);
        }
        
        Page<SysUserVO> voPage = new Page<>(current, size);
        voPage.setRecords(voList);
        voPage.setTotal(page.getTotal());
        voPage.setPages(page.getPages());
        
        return Result.success(voPage);
    }

    /**
     * 2. 新增管理员账号 (B 端)
     */
    @PostMapping("/create")
    public Result<?> create(@RequestBody SysUser sysUser) {
        if (sysUser == null || !StringUtils.hasText(sysUser.getUsername()) || !StringUtils.hasText(sysUser.getPassword())) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "用户名和初始密码不能为空");
        }
        
        // 校验唯一性
        SysUser exist = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, sysUser.getUsername().trim())
        );
        if (exist != null) {
            return Result.error(ResultCode.ERROR, "登录账号已存在，不能重复注册！");
        }
        
        sysUser.setUsername(sysUser.getUsername().trim());
        sysUser.setUserType("ADMIN"); // 强制为管理端用户
        if (sysUser.getStatus() == null) {
            sysUser.setStatus(0); // 默认启用
        }
        
        // 密码哈希自适应
        String rawPassword = sysUser.getPassword();
        if (securityProperties.isPasswordEncryptEnabled()) {
            sysUser.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        }
        
        sysUserService.save(sysUser);
        return Result.success("管理员账号新增成功");
    }

    /**
     * 3. 编辑修改用户信息 (安全防御：不允许自残和禁用自我)
     */
    @PutMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(@RequestBody SysUser sysUser) {
        if (sysUser == null || sysUser.getId() == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "用户ID不能为空");
        }
        
        Long loginId = StpUtil.getLoginIdAsLong();
        
        // 防守校验：如果修改的是自己当前登录 of 账号
        if (sysUser.getId().equals(loginId)) {
            if (sysUser.getStatus() != null && sysUser.getStatus() == 1) {
                return Result.error(ResultCode.ERROR, "安全保护：当前登录管理员不能将自我账号更新为禁用状态！");
            }
            if (sysUser.getUserType() != null && "USER".equals(sysUser.getUserType().toUpperCase())) {
                return Result.error(ResultCode.ERROR, "安全保护：当前登录管理员不能将自我更新降级为普通 C 端用户！");
            }
        }
        
        SysUser exist = sysUserService.getById(sysUser.getId());
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "待编辑的用户不存在");
        }
        
        if (StringUtils.hasText(sysUser.getUsername())) {
            // 校验唯一
            SysUser usernameConflict = sysUserService.getOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, sysUser.getUsername().trim())
                            .ne(SysUser::getId, sysUser.getId())
            );
            if (usernameConflict != null) {
                return Result.error(ResultCode.ERROR, "登录账号已被其他账户占用");
            }
            exist.setUsername(sysUser.getUsername().trim());
        }
        
        if (sysUser.getStatus() != null) {
            exist.setStatus(sysUser.getStatus());
        }
        if (sysUser.getUserType() != null) {
            exist.setUserType(sysUser.getUserType().toUpperCase());
        }
        
        sysUserService.updateById(exist);
        
        // 网关缓存清除防御
        sysApiService.clearUserApiCache(sysUser.getId());
        
        return Result.success("更新用户信息成功");
    }

    /**
     * 4. 重置/修改他人密码
     */
    @PostMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody ResetPasswordParam param) {
        if (param == null || param.getUserId() == null || !StringUtils.hasText(param.getNewPassword())) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "userId 和新密码均不能为空");
        }
        
        SysUser exist = sysUserService.getById(param.getUserId());
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "用户不存在");
        }
        
        String newPassword = param.getNewPassword();
        if (securityProperties.isPasswordEncryptEnabled()) {
            exist.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        } else {
            exist.setPassword(newPassword);
        }
        
        sysUserService.updateById(exist);
        
        // 强制踢其下线，清除一切缓存
        StpUtil.kickout(param.getUserId());
        sysApiService.clearUserApiCache(param.getUserId());
        
        return Result.success("重置该用户密码成功，已强制该用户下线并重置网关缓存。");
    }

    /**
     * 5. 删除用户 (防自残自毁、社交账号与角色极速级联物理清除)
     */
    @DeleteMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@PathVariable Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "待删除的用户ID不能为空");
        }
        
        Long loginId = StpUtil.getLoginIdAsLong();
        
        // 核心硬防守：严禁当前正在登录的用户通过接口将自我毁灭
        if (id.equals(loginId)) {
            throw new BusinessException("安全防御：您无法通过 API 物理删除您当前正在登录的管理员账号！");
        }
        
        SysUser exist = sysUserService.getById(id);
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "该用户已经不存在");
        }
        
        // 1. 物理删除用户
        sysUserService.removeById(id);
        
        // 2. 极速级联物理清空绑定的角色关系 sys_user_role
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
        );
        
        // 3. 极速级联物理清空其在 sys_user_social 绑定的所有第三方账号关系
        sysUserSocialMapper.delete(
                new LambdaQueryWrapper<SysUserSocial>().eq(SysUserSocial::getUserId, id)
        );
        
        // 4. 清除该用户的 Redis 网关拦截缓存并强制退登
        sysApiService.clearUserApiCache(id);
        StpUtil.logout(id);
        
        log.info(">>>>>> [物理级联删除用户] 用户 {} 被管理员 {} 物理销毁。级联清空其角色映射、第三方社交映射、Redis缓存与会话", exist.getUsername(), loginId);
        return Result.success("用户及其所有级联角色绑定、社交账号绑定 and 网关缓存已全部清除成功。");
    }

    /**
     * 6. 管理员个人自维护：修改自身信息
     */
    @PutMapping("/my-info")
    public Result<?> updateMyInfo(@RequestBody SysUser sysUser) {
        Long loginId = StpUtil.getLoginIdAsLong();
        
        SysUser exist = sysUserService.getById(loginId);
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "您的当前登录会话无效，可能账号已被删除");
        }
        
        // 只能修改用户名，且不能降级自我或将自我禁用
        if (sysUser != null && StringUtils.hasText(sysUser.getUsername())) {
            String newUsername = sysUser.getUsername().trim();
            // 校验唯一
            SysUser usernameConflict = sysUserService.getOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, newUsername)
                            .ne(SysUser::getId, loginId)
            );
            if (usernameConflict != null) {
                return Result.error(ResultCode.ERROR, "修改后的账号已被他人占用");
            }
            exist.setUsername(newUsername);
        }
        
        sysUserService.updateById(exist);
        return Result.success("个人基本信息修改成功");
    }

    /**
     * 7. 管理员个人自维护：修改自身密码 (具有自适应密码哈希碰撞比对)
     */
    @PostMapping("/my-password")
    public Result<?> updateMyPassword(@RequestBody UpdatePasswordParam param) {
        if (param == null || !StringUtils.hasText(param.getOldPassword()) || !StringUtils.hasText(param.getNewPassword())) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "旧密码和新密码均不能为空");
        }
        
        Long loginId = StpUtil.getLoginIdAsLong();
        SysUser exist = sysUserService.getById(loginId);
        if (exist == null) {
            return Result.error(ResultCode.ERROR, "会话已失效，找不到当前登录账户");
        }
        
        // 比对旧密码（碰撞校验）
        String dbPassword = exist.getPassword();
        String oldPassword = param.getOldPassword();
        
        boolean match = false;
        if (securityProperties.isPasswordEncryptEnabled()) {
            match = BCrypt.checkpw(oldPassword, dbPassword);
        } else {
            match = oldPassword.equals(dbPassword);
        }
        
        if (!match) {
            return Result.error(ResultCode.ERROR, "旧密码输入不正确，请重新输入");
        }
        
        // 更新为新密码
        String newPassword = param.getNewPassword();
        if (securityProperties.isPasswordEncryptEnabled()) {
            exist.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        } else {
            exist.setPassword(newPassword);
        }
        
        sysUserService.updateById(exist);
        
        // 强制踢自己下线，重登录
        StpUtil.logout();
        sysApiService.clearUserApiCache(loginId);
        
        return Result.success("密码修改成功，请使用新密码重新登录！");
    }

    /**
     * 8. 管理员个人自维护：获取个人完整信息 (含基本资料、角色、接口拦截权限清单)
     */
    @GetMapping("/profile")
    public Result<AdminProfileVO> getMyProfile() {
        Long loginId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(loginId);
        if (user == null) {
            return Result.error(ResultCode.ERROR, "会话已失效，找不到当前登录账户");
        }

        AdminProfileVO profile = new AdminProfileVO();

        // 1. 基本资料装配
        SysUserVO uVo = new SysUserVO();
        uVo.setId(user.getId());
        uVo.setUsername(user.getUsername());
        uVo.setUserType(user.getUserType());
        uVo.setStatus(user.getStatus());
        uVo.setCreateTime(user.getCreateTime());
        uVo.setUpdateTime(user.getUpdateTime());

        // 查询社交绑定
        List<SysUserSocial> socials = sysUserSocialMapper.selectList(
                new LambdaQueryWrapper<SysUserSocial>().eq(SysUserSocial::getUserId, loginId)
        );
        uVo.setSocials(socials);

        // 2. 角色列表装配
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, loginId)
        );
        List<SysRole> roles = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userRoles)) {
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)
            );
        }
        uVo.setRoles(roles);
        profile.setUserInfo(uVo);
        profile.setRoles(roles);

        // 3. API 权限清单装配
        Set<String> apiPerms = sysApiService.getApiPermissionsByUserId(loginId);
        profile.setPermissions(apiPerms);

        return Result.success(profile);
    }

    @Data
    public static class AdminProfileVO {
        private SysUserVO userInfo;
        private List<SysRole> roles;
        private Set<String> permissions;
    }

    @Data
    public static class SysUserVO {
        private Long id;
        private String username;
        private String userType;
        private Integer status;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
        private List<SysUserSocial> socials;
        private List<SysRole> roles;
    }

    @Data
    public static class ResetPasswordParam {
        private Long userId;
        private String newPassword;
    }

    @Data
    public static class UpdatePasswordParam {
        private String oldPassword;
        private String newPassword;
    }
}
