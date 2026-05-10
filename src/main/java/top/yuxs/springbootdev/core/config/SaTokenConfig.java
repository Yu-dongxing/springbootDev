/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.FirewallCheckException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.enums.ResultCode;

/**
 * Sa-Token 鉴权配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

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
     * 注册 Sa-Token 拦截器，统一处理登录校验
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter
                        .match("/**")
//                        .notMatch("/api/user/init")
//                        .notMatch("/api/user/login")
//                        .notMatch("/api/user/register")
                        .notMatch("/uploads/**")
                        .notMatch("/api/common/**")
//                        .notMatch("/swagger-ui/**")
                        .notMatch(SaHttpMethod.OPTIONS)
                        .check(r -> StpUtil.checkLogin())
                ))
                .addPathPatterns("/**");
    }
}
