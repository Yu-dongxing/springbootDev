/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/01
 */

package top.yuxs.springbootdev.modules.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.asymmetric.RSA;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.config.AegisSecurityProperties;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.core.utils.StpUserUtil;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.mapper.SysUserRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;
import top.yuxs.springbootdev.modules.system.service.SysUserService;
import top.yuxs.springbootdev.modules.system.service.SysApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统安全授权与认证 控制器
 * 提供：获取传输加密公钥、C端用户注册、C端用户登录、B端管理端登录等接口
 *
 * @author YuDongXing
 * @since 2026/06/01
 */
@Slf4j
@RestController
@RequestMapping("/api/common/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AegisSecurityProperties securityProperties;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysApiService sysApiService;

    /**
     * 获取前端密码传输加密开启状态
     */
    @GetMapping("/frontend-encrypt/status")
    public Result<Boolean> getFrontendEncryptStatus() {
        return Result.success(securityProperties.isFrontendEncryptEnabled());
    }

    /**
     * 获取用于前端网络传输加密的 RSA 公钥
     * 极防中间人嗅探安全标准
     */
    @GetMapping("/rsa/public-key")
    public Result<String> getRsaPublicKey() {
        if (!securityProperties.isFrontendEncryptEnabled()) {
            throw new BusinessException("系统未开启前端密码传输加密服务");
        }
        String publicKey = sysConfigService.getValue("sys.auth.rsa.public-key");
        if (publicKey == null) {
            log.info(">>>>>> 数据库公钥记录为空，触发自适应热生成高安全 2048 位 RSA 密钥对...");
            RSA rsa = new RSA();
            sysConfigService.updateConfig("sys.auth.rsa.private-key", rsa.getPrivateKeyBase64(),
                    "密码安全传输 RSA 私钥", "用于解密前端用公钥加密后的密码密文，必须严防泄露");
            sysConfigService.updateConfig("sys.auth.rsa.public-key", rsa.getPublicKeyBase64(),
                    "密码安全传输 RSA 公钥", "提供给前端对传输密码原文进行非对称加密的公钥");
            publicKey = rsa.getPublicKeyBase64();
        }
        return Result.success(publicKey);
    }

    /**
     * C端普通用户端 密码安全注册
     */
    @PostMapping("/user/register")
    public Result<?> registerUser(@Validated @RequestBody AuthParam param) {
        log.info(">>>>>> 收到普通用户注册请求, 账号: {}", param.getUsername());
        sysUserService.registerUser(param.getUsername(), param.getPassword());
        return Result.success();
    }

    /**
     * C端普通用户端 密码安全登录
     */
    @PostMapping("/user/login")
    public Result<LoginResultVO> loginUser(@Validated @RequestBody AuthParam param) {
        log.info(">>>>>> 收到普通用户登录请求, 账号: {}", param.getUsername());
        String token = sysUserService.loginUser(param.getUsername(), param.getPassword());

        Long loginUserId = StpUserUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(loginUserId);

        LoginResultVO vo = new LoginResultVO();
        vo.setToken(token);

        SysUserVO userInfo = new SysUserVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setUserType(user.getUserType());
        userInfo.setStatus(user.getStatus());
        userInfo.setCreateTime(user.getCreateTime());
        userInfo.setUpdateTime(user.getUpdateTime());
        vo.setUserInfo(userInfo);

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
        vo.setRoles(roleKeys);

        Set<String> apiPerms = sysApiService.getApiPermissionsByUserId(loginUserId);
        vo.setPermissions(new ArrayList<>(apiPerms));

        return Result.success(vo);
    }

    /**
     * B端管理端 物理隔离安全登录
     */
    @PostMapping("/admin/login")
    public Result<LoginResultVO> loginAdmin(@Validated @RequestBody AuthParam param) {
        log.info(">>>>>> 收到管理端登录请求, 账号: {}", param.getUsername());
        String token = sysUserService.loginAdmin(param.getUsername(), param.getPassword());

        Long loginId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(loginId);

        LoginResultVO vo = new LoginResultVO();
        vo.setToken(token);

        SysUserVO userInfo = new SysUserVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setUserType(user.getUserType());
        userInfo.setStatus(user.getStatus());
        userInfo.setCreateTime(user.getCreateTime());
        userInfo.setUpdateTime(user.getUpdateTime());
        vo.setUserInfo(userInfo);

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, loginId)
        );
        List<String> roleKeys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userRoles)) {
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            List<SysRole> roles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)
            );
            roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList());
        }
        vo.setRoles(roleKeys);

        Set<String> apiPerms = sysApiService.getApiPermissionsByUserId(loginId);
        vo.setPermissions(new ArrayList<>(apiPerms));

        return Result.success(vo);
    }

    /**
     * 内部登录参数交互 DTO 结构
     */
    @Data
    public static class AuthParam {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password; // 前端 RSA 散列强非对称密文 Base64 字符串
    }

    /**
     * 登录成功响应数据载体
     */
    @Data
    public static class LoginResultVO {
        private String token;
        private SysUserVO userInfo;
        private List<String> roles;
        private List<String> permissions;
    }

    /**
     * 登录用户基本信息 VO 结构 (防止精度丢失)
     */
    @Data
    public static class SysUserVO {
        private Long id;
        private String username;
        private String userType;
        private Integer status;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
    }
}
