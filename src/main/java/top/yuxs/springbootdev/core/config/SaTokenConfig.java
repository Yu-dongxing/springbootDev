/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.FirewallCheckException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.core.utils.StpUserUtil;
import top.yuxs.springbootdev.modules.system.service.SysApiService;
import top.yuxs.springbootdev.modules.system.service.SysRoleService;

import java.util.Set;

/**
 * Sa-Token 全局权限网关拦截鉴权配置
 * 实现：
 * 1. 双端 Token 账号隔离 (B端 StpUtil / C端 StpUserUtil)
 * 2. 物理 API 动态路由拦截器 (零硬编码注解, 基于 Path + Method 动态拦截，高灵敏防御)
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    // 采用 @Lazy 延迟注入，防止 Spring 启动中与安全服务、扫描器产生循环依赖引用
    @Autowired
    @Lazy
    private SysApiService sysApiService;

    @Autowired
    @Lazy
    private SysRoleService sysRoleService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 注册 Sa-Token 全局过滤器，拦截防火墙异常并返回标准 JSON
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                    // 手动检查请求路径中的双斜杠 (Sa-Token 默认可能拦截但不抛出异常)
                    String path = SaHolder.getRequest().getRequestPath();
                    if (path.contains("//")) {
                        throw new FirewallCheckException("非法请求路径: " + path);
                    }
                })
                .setError(e -> {
                    // 设置响应头为 JSON
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=utf-8");
                    // 返回标准 Result 格式
                    return JSON.toJSONString(Result.error(ResultCode.FORBIDDEN.getCode(), "请求被防火墙拦截: " + e.getMessage()));
                });
    }

    /**
     * 注册 Sa-Token 网关拦截器，统一处理双端隔离与免注解物理接口动态匹配校验
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            String path = SaHolder.getRequest().getRequestPath();
            String method = SaHolder.getRequest().getMethod().toUpperCase();

            // 1. 全局公共白名单放行（如静态资源、第三方登录引导及回调逻辑、OPTIONS 预检等，无需登录）
            if (SaRouter.match(
                    "/uploads/**",
                    "/api/common/oauth/**", // 第三方 OAuth 核心渲染与回调放行
                    "/api/common/auth/**",  // 统一安全注册、登录、拉取 RSA 公钥放行
                    "/api/common/public/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/favicon.ico"
            ).isHit() || SaHttpMethod.OPTIONS.name().equals(method)) {
                return;
            }

            // 2. B 端管理端 (ADMIN) 网关级精细防守与免注解鉴权
            if (pathMatcher.match("/api/admin/**", path)) {
                
                // a. 排除管理端登录鉴权白名单接口
                if (SaRouter.match("/api/admin/auth/login", "/api/admin/auth/register").isHit()) {
                    return;
                }

                // b. 强制检查管理员登录状态 (loginType = "login")
                StpUtil.checkLogin();

                // c. 超级管理员特权直接无条件放行，极大降低高并发压力下的性能开销
                Long userId = StpUtil.getLoginIdAsLong();
                if (sysRoleService.isSuperAdmin(userId)) {
                    return;
                }

                // d. 普通管理员：基于 Redis 缓存的物理 API 动态权限多匹配
                Set<String> apiPermissions = sysApiService.getApiPermissionsByUserId(userId);
                
                boolean hasAccess = false;
                for (String perm : apiPermissions) {
                    // 规则格式为 "METHOD:PATH"，如 "GET:/api/admin/sys-user/*"
                    String[] parts = perm.split(":", 2);
                    if (parts.length < 2) {
                        continue;
                    }
                    String ruleMethod = parts[0].toUpperCase();
                    String rulePath = parts[1];

                    // 方法比对 (* 代表通配所有请求方式)
                    if ("*".equals(ruleMethod) || ruleMethod.equals(method)) {
                        // 路径比对 (使用 Spring 原生 AntPathMatcher 匹配器)
                        if (pathMatcher.match(rulePath, path)) {
                            hasAccess = true;
                            break;
                        }
                    }
                }

                // e. 强行阻拦越权调用并抛出异常
                if (!hasAccess) {
                    throw new NotPermissionException("无此后台接口的操作访问权限 [" + method + " " + path + "]");
                }
                return;
            }

            // 3. C 端普通用户端 (USER) 网关级防守
            if (pathMatcher.match("/api/user/**", path)) {
                
                // 排除用户端无需登录的账户初始与登录接口
                if (SaRouter.match("/api/user/login", "/api/user/register").isHit()) {
                    return;
                }

                // 强制检查 C 端用户登录状态 (loginType = "user"，完美实现物理双端会话及主键隔离)
                StpUserUtil.checkLogin();
            }

        })).addPathPatterns("/**");
    }
}
