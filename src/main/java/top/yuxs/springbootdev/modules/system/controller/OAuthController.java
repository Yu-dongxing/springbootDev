/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.enums.SocialPlatformEnum;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;
import top.yuxs.springbootdev.modules.system.service.SysUserSocialService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 第三方社交登录与注册 统一控制层
 * 支持任意符合 JustAuth 的第三方平台一键授权引导与回调绑定
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Slf4j
@RestController
@RequestMapping("/api/common/oauth")
public class OAuthController {

    @Autowired
    private SysUserSocialService sysUserSocialService;

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 获取系统支持且由后台启用的第三方社交登录平台列表
     * 支持前端动态渲染
     *
     * @author YuDongXing
     * @since 2026/06/01
     */
    @GetMapping("/platforms")
    public Result<List<SocialPlatformVO>> getActivePlatforms() {
        log.info(">>>>>> 收到前端获取第三方可用登录通道列表请求...");
        List<SocialPlatformVO> activeList = new ArrayList<>();
        
        for (SocialPlatformEnum platform : SocialPlatformEnum.values()) {
            // 查库获取是否开启及是否配置秘钥
            String clientId = sysConfigService.getValue(platform.getClientIdKey());
            String clientSecret = sysConfigService.getValue(platform.getClientSecretKey());
            String enabledVal = sysConfigService.getValue(platform.getEnabledKey(), "false");
            
            boolean enabled = "true".equalsIgnoreCase(enabledVal) 
                    && StringUtils.hasText(clientId) 
                    && StringUtils.hasText(clientSecret);
            
            SocialPlatformVO vo = new SocialPlatformVO();
            vo.setCode(platform.getCode());
            vo.setName(platform.getName());
            vo.setEnabled(enabled);
            activeList.add(vo);
        }
        
        return Result.success(activeList);
    }

    /**
     * 社交平台可用性返回视图
     */
    @Data
    public static class SocialPlatformVO {
        private String code;     // 前端标识，如 github, gitee
        private String name;     // 友好名称，如 GitHub, Gitee
        private boolean enabled; // 开关状态
    }

    /**
     * 构建授权引导 URL 并返回
     *
     * @param source 平台标志，如 github, gitee
     * @return 统一格式响应，包含重定向授权地址
     * @author YuDongXing
     * @since 2026/05/31
     */
    @GetMapping("/render/{source}")
    public Result<String> renderAuth(@PathVariable String source) {
        log.info(">>>>>> 收到第三方授权引导请求, 目标平台: {}", source);
        AuthRequest authRequest = sysUserSocialService.getAuthRequest(source);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return Result.success(authorizeUrl);
    }

    /**
     * 统一 OAuth 授权成功后的回调接收端点
     * 自动实现：三方数据对碰 -> 未绑定无感注册并建立映射 -> 登录并派发 C端 Token 凭证
     * 并在后端直接展示极具 2026 现代化美学的毛玻璃毛态响应页面，供演示或用户直接获取凭证
     *
     * @param source   平台标志
     * @param callback JustAuth 统一回调对象
     * @param response 响应对象
     * @author YuDongXing
     * @since 2026/05/31
     */
    @GetMapping("/callback/{source}")
    public void callback(@PathVariable String source, AuthCallback callback, HttpServletResponse response) throws IOException {
        log.info(">>>>>> 收到第三方授权回调, 来源平台: {}, State: {}, Code: {}", source, callback.getState(), callback.getCode());
        response.setContentType("text/html;charset=utf-8");
        PrintWriter writer = response.getWriter();

        try {
            AuthRequest authRequest = sysUserSocialService.getAuthRequest(source);
            AuthResponse<AuthUser> authResponse = authRequest.login(callback);

            if (authResponse.ok()) {
                AuthUser authUser = authResponse.getData();
                log.info(">>>>>> 第三方授权交互成功! 昵称: {}, 唯一ID: {}", authUser.getNickname(), authUser.getUuid());

                // 完成无感注册绑定并派发 C 端 Token 会话
                String token = sysUserSocialService.handleSocialLoginOrRegister(source, authUser);
                log.info(">>>>>> 系统已成功自动分配本地会话凭证 Token: {}", token);

                // 渲染极其奢华现代的玻璃微拟态 UI 响应页面
                writer.write(getSuccessHtml(source, authUser, token));
            } else {
                log.error(">>>>>> 第三方登录回调获取用户信息失败! 错误码: {}, 原因: {}", authResponse.getCode(), authResponse.getMsg());
                writer.write(getErrorHtml(source, authResponse.getMsg()));
            }
        } catch (Exception e) {
            log.error(">>>>>> 统一回调处理发生内部系统异常!", e);
            writer.write(getErrorHtml(source, e.getMessage()));
        } finally {
            writer.flush();
        }
    }

    /**
     * 渲染 2026 级现代奢华极光暗黑毛玻璃风格的授权成功响应 HTML 页面
     */
    private String getSuccessHtml(String source, AuthUser authUser, String token) {
        String platformName = source.toUpperCase();
        String accentColor = "linear-gradient(135deg, #6366f1, #a855f7, #ec4899)";
        String platformColor = "#24292e"; // 默认 GitHub 色
        if ("GITEE".equalsIgnoreCase(source)) {
            platformColor = "#fe7300";
        } else if ("WECHAT".equalsIgnoreCase(source)) {
            platformColor = "#07c160";
        }

        String template = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>授权登录成功 | Aegis-Boot</title>
                    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&family=Noto+Sans+SC:wght@300;400;700&display=swap" rel="stylesheet">
                    <style>
                        * {
                            box-sizing: border-box;
                            margin: 0;
                            padding: 0;
                        }
                        body {
                            background-color: #0b0f19;
                            background-image: 
                                radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.15) 0%, transparent 40%),
                                radial-gradient(circle at 90% 80%, rgba(236, 72, 153, 0.12) 0%, transparent 40%);
                            color: #f3f4f6;
                            font-family: 'Outfit', 'Noto Sans SC', sans-serif;
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            overflow-x: hidden;
                            position: relative;
                        }
                        /* 背景流光装饰 */
                        .aurora-blur-1 {
                            position: absolute;
                            width: 300px;
                            height: 300px;
                            background: #6366f1;
                            filter: blur(120px);
                            top: 20%;
                            left: 20%;
                            border-radius: 50%;
                            z-index: 0;
                            opacity: 0.3;
                            animation: float1 10s ease-in-out infinite alternate;
                        }
                        .aurora-blur-2 {
                            position: absolute;
                            width: 250px;
                            height: 250px;
                            background: #d946ef;
                            filter: blur(120px);
                            bottom: 20%;
                            right: 25%;
                            border-radius: 50%;
                            z-index: 0;
                            opacity: 0.2;
                            animation: float2 8s ease-in-out infinite alternate;
                        }
                        @keyframes float1 {
                            0% { transform: translate(0, 0) scale(1); }
                            100% { transform: translate(50px, 30px) scale(1.2); }
                        }
                        @keyframes float2 {
                            0% { transform: translate(0, 0) scale(1); }
                            100% { transform: translate(-40px, -20px) scale(1.15); }
                        }

                        /* 玻璃体面板 */
                        .glass-container {
                            background: rgba(17, 24, 39, 0.45);
                            backdrop-filter: blur(24px);
                            -webkit-backdrop-filter: blur(24px);
                            border: 1px solid rgba(255, 255, 255, 0.08);
                            border-radius: 28px;
                            padding: 45px 40px;
                            width: 90%;
                            max-width: 480px;
                            box-shadow: 
                                0 4px 30px rgba(0, 0, 0, 0.4),
                                inset 0 1px 1px rgba(255, 255, 255, 0.05);
                            text-align: center;
                            z-index: 1;
                            opacity: 0;
                            transform: translateY(30px);
                            animation: fadeInUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
                        }
                        @keyframes fadeInUp {
                            to {
                                opacity: 1;
                                transform: translateY(0);
                            }
                        }

                        /* 头像酷炫边框 */
                        .avatar-wrapper {
                            position: relative;
                            width: 104px;
                            height: 104px;
                            margin: 0 auto 24px;
                        }
                        .avatar-gradient-border {
                            position: absolute;
                            top: 0;
                            left: 0;
                            right: 0;
                            bottom: 0;
                            border-radius: 50%;
                            background: %s;
                            padding: 3px;
                            animation: rotateBorder 12s linear infinite;
                        }
                        .avatar-inner {
                            width: 100%;
                            height: 100%;
                            border-radius: 50%;
                            background: #111827;
                            padding: 2px;
                        }
                        .avatar-img {
                            width: 100%;
                            height: 100%;
                            border-radius: 50%;
                            object-fit: cover;
                        }
                        @keyframes rotateBorder {
                            0% { transform: rotate(0deg); }
                            100% { transform: rotate(360deg); }
                        }

                        /* 平台微章 */
                        .platform-badge {
                            display: inline-block;
                            padding: 4px 12px;
                            border-radius: 30px;
                            font-size: 11px;
                            font-weight: 800;
                            text-transform: uppercase;
                            letter-spacing: 1.5px;
                            margin-bottom: 12px;
                            color: #ffffff;
                            background-color: %s;
                            border: 1px solid rgba(255, 255, 255, 0.15);
                            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
                        }

                        /* 标题和文字 */
                        h1 {
                            font-size: 24px;
                            font-weight: 800;
                            letter-spacing: -0.5px;
                            margin-bottom: 8px;
                            background: linear-gradient(to right, #ffffff, #d1d5db);
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                        }
                        .nickname {
                            color: #818cf8;
                            font-weight: 600;
                        }
                        p.subtitle {
                            font-size: 14px;
                            color: #9ca3af;
                            margin-bottom: 32px;
                        }

                        /* Token 展现框 */
                        .token-section {
                            background: rgba(0, 0, 0, 0.35);
                            border: 1px solid rgba(255, 255, 255, 0.05);
                            border-radius: 16px;
                            padding: 16px;
                            margin-bottom: 24px;
                            text-align: left;
                            position: relative;
                        }
                        .token-label {
                            font-size: 11px;
                            text-transform: uppercase;
                            letter-spacing: 1px;
                            color: #6b7280;
                            margin-bottom: 8px;
                            display: block;
                            font-weight: 600;
                        }
                        .token-display-container {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                        }
                        .token-value {
                            font-family: 'Courier New', Courier, monospace;
                            font-size: 13px;
                            color: #a5b4fc;
                            word-break: break-all;
                            white-space: pre-wrap;
                            max-height: 52px;
                            overflow-y: auto;
                            padding-right: 12px;
                            flex-grow: 1;
                        }
                        .token-value::-webkit-scrollbar {
                            width: 4px;
                        }
                        .token-value::-webkit-scrollbar-thumb {
                            background: rgba(255, 255, 255, 0.1);
                            border-radius: 10px;
                        }

                        /* 复制按钮 */
                        .copy-btn {
                            background: rgba(255, 255, 255, 0.08);
                            border: 1px solid rgba(255, 255, 255, 0.1);
                            color: #f3f4f6;
                            padding: 8px 12px;
                            border-radius: 10px;
                            font-size: 12px;
                            font-weight: 600;
                            cursor: pointer;
                            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
                            flex-shrink: 0;
                            display: flex;
                            align-items: center;
                            gap: 4px;
                        }
                        .copy-btn:hover {
                            background: rgba(255, 255, 255, 0.15);
                            border-color: rgba(255, 255, 255, 0.2);
                            transform: scale(1.03);
                        }
                        .copy-btn:active {
                            transform: scale(0.97);
                        }
                        .copy-btn.copied {
                            background: #10b981;
                            border-color: #10b981;
                            color: white;
                        }

                        /* 下一步指引 */
                        .tip-box {
                            background: rgba(99, 102, 241, 0.06);
                            border: 1px dashed rgba(99, 102, 241, 0.25);
                            border-radius: 12px;
                            padding: 12px 16px;
                            font-size: 12px;
                            color: #a5b4fc;
                            line-height: 1.5;
                        }

                        /* 关闭/返回提示 */
                        .close-tip {
                            font-size: 12px;
                            color: #4b5563;
                            margin-top: 24px;
                        }
                    </style>
                </head>
                <body>
                    <div class="aurora-blur-1"></div>
                    <div class="aurora-blur-2"></div>

                    <div class="glass-container">
                        <div class="avatar-wrapper">
                            <div class="avatar-gradient-border"></div>
                            <div class="avatar-inner">
                                <img class="avatar-img" src="%s" alt="Social Avatar" onerror="this.src='https://api.dicebear.com/7.x/bottts/svg?seed=Aegis'">
                            </div>
                        </div>

                        <span class="platform-badge">%s 授权成功</span>
                        <h1>欢迎，<span class="nickname">%s</span></h1>
                        <p class="subtitle">您的账户已成功免密注册并安全绑定本地系统</p>

                        <div class="token-section">
                            <span class="token-label">本地会话凭证 (Token)</span>
                            <div class="token-display-container">
                                <div class="token-value" id="tokenText">%s</div>
                                <button class="copy-btn" id="copyBtn" onclick="copyToken()">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
                                    <span>复制</span>
                                </button>
                            </div>
                        </div>

                        <div class="tip-box">
                            💡 <strong>下一步操作提示：</strong><br>
                            该 Token 代表您在 C 端的专属会话状态。您可以将其添加到请求头 <code>satoken: %s</code>，以便安全调用 C 端 <code>/api/user/**</code> 受限业务接口。
                        </div>

                        <div class="close-tip">
                            您可以关闭当前授权页面返回客户端应用
                        </div>
                    </div>

                    <script>
                        function copyToken() {
                            const tokenVal = document.getElementById('tokenText').innerText;
                            navigator.clipboard.writeText(tokenVal).then(() => {
                                const btn = document.getElementById('copyBtn');
                                const textSpan = btn.querySelector('span');
                                
                                btn.classList.add('copied');
                                textSpan.innerText = '已复制';
                                
                                setTimeout(() => {
                                    btn.classList.remove('copied');
                                    textSpan.innerText = '复制';
                                }, 2000);
                            }).catch(err => {
                                alert('复制失败，请手动选择复制。');
                            });
                        }
                    </script>
                </body>
                </html>
                """;

        return String.format(template, accentColor, platformColor, authUser.getAvatar(), platformName, authUser.getNickname(), token, token);
    }

    /**
     * 渲染 2026 级现代奢华珊瑚红风格的授权失败响应 HTML 页面
     */
    private String getErrorHtml(String source, String errorMsg) {
        String platformName = source != null ? source.toUpperCase() : "第三方";
        String template = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>授权登录失败 | Aegis-Boot</title>
                    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&family=Noto+Sans+SC:wght@300;400;700&display=swap" rel="stylesheet">
                    <style>
                        * {
                            box-sizing: border-box;
                            margin: 0;
                            padding: 0;
                        }
                        body {
                            background-color: #0b0f19;
                            background-image: 
                                radial-gradient(circle at 10% 20%, rgba(239, 68, 68, 0.1) 0%, transparent 40%);
                            color: #f3f4f6;
                            font-family: 'Outfit', 'Noto Sans SC', sans-serif;
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            overflow-x: hidden;
                        }
                        .glass-container {
                            background: rgba(17, 24, 39, 0.45);
                            backdrop-filter: blur(24px);
                            -webkit-backdrop-filter: blur(24px);
                            border: 1px solid rgba(239, 68, 68, 0.15);
                            border-radius: 28px;
                            padding: 45px 40px;
                            width: 90%;
                            max-width: 480px;
                            box-shadow: 0 4px 30px rgba(0, 0, 0, 0.4);
                            text-align: center;
                            position: relative;
                        }
                        .error-icon-wrapper {
                            width: 80px;
                            height: 80px;
                            border-radius: 50%;
                            background: rgba(239, 68, 68, 0.1);
                            border: 1px solid rgba(239, 68, 68, 0.3);
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            margin: 0 auto 24px;
                        }
                        .error-icon {
                            color: #f87171;
                        }
                        h1 {
                            font-size: 22px;
                            font-weight: 800;
                            margin-bottom: 8px;
                            color: #f87171;
                        }
                        p.subtitle {
                            font-size: 14px;
                            color: #9ca3af;
                            margin-bottom: 24px;
                        }
                        .error-details {
                            background: rgba(0, 0, 0, 0.25);
                            border: 1px solid rgba(239, 68, 68, 0.1);
                            border-radius: 12px;
                            padding: 14px;
                            font-family: monospace;
                            font-size: 12px;
                            color: #fca5a5;
                            word-break: break-all;
                            text-align: left;
                            margin-bottom: 24px;
                        }
                        .back-btn {
                            background: linear-gradient(135deg, #ef4444, #f43f5e);
                            border: none;
                            color: white;
                            padding: 12px 24px;
                            border-radius: 12px;
                            font-size: 14px;
                            font-weight: 600;
                            cursor: pointer;
                            transition: all 0.2s;
                            box-shadow: 0 4px 14px rgba(239, 68, 68, 0.3);
                            text-decoration: none;
                            display: inline-block;
                        }
                        .back-btn:hover {
                            transform: scale(1.03);
                            box-shadow: 0 6px 20px rgba(239, 68, 68, 0.4);
                        }
                    </style>
                </head>
                <body>
                    <div class="glass-container">
                        <div class="error-icon-wrapper">
                            <svg class="error-icon" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>
                        </div>
                        <h1>%s 授权登录失败</h1>
                        <p class="subtitle">在与第三方 OAuth 平台握手时发生未知异常</p>
                        
                        <div class="error-details">
                            <strong>ERROR:</strong><br>
                            %s
                        </div>

                        <a href="javascript:history.back()" class="back-btn">重新尝试授权</a>
                    </div>
                </body>
                </html>
                """;
        return String.format(template, platformName, errorMsg);
    }
}
