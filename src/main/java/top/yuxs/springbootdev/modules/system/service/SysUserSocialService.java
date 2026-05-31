/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import top.yuxs.springbootdev.modules.system.entity.SysUserSocial;

/**
 * 用户第三方社交账号绑定 服务类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
public interface SysUserSocialService extends IService<SysUserSocial> {

    /**
     * 根据第三方登录源（如 github, gitee），动态构建并装配 JustAuth 的 AuthRequest
     */
    AuthRequest getAuthRequest(String source);

    /**
     * 处理第三方授权成功后的登录/注册绑定闭环逻辑 (返回 C 端用户的登录 Token 凭证)
     */
    String handleSocialLoginOrRegister(String source, AuthUser authUser);
}
