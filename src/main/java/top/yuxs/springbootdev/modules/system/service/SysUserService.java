/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.yuxs.springbootdev.modules.system.entity.SysUser;

/**
 * 系统统一用户 服务类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 普通用户端 (C端) 注册账户
     * 接收前端经 RSA 公钥加密后的密码密文，在后端解密，再经 BCrypt 哈希加盐持久化。
     *
     * @param username 用户名
     * @param encryptedPassword RSA 加密后的密码 Base64 密文
     */
    void registerUser(String username, String encryptedPassword);

    /**
     * 普通用户端 (C端) 密码登录
     * 限制仅 user_type = USER 的账号可登录。
     *
     * @param username 用户名
     * @param encryptedPassword RSA 加密后的密码 Base64 密文
     * @return 登录成功后的 C端 Token 凭证
     */
    String loginUser(String username, String encryptedPassword);

    /**
     * 管理端 (B端) 密码安全登录
     * 限制仅 user_type = ADMIN 的账号可登录。
     *
     * @param username 用户名
     * @param encryptedPassword RSA 加密后的密码 Base64 密文
     * @return 登录成功后的 B端 Token 凭证
     */
    String loginAdmin(String username, String encryptedPassword);

    /**
     * 解密前端传入的 RSA 密码密文
     *
     * @param encryptedPassword RSA 加密后的密码 Base64 密文
     * @return 解密后的密码明文
     */
    String decryptPassword(String encryptedPassword);
}
