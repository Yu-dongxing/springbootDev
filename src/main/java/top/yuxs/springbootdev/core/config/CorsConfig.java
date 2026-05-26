/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/11
 */

package top.yuxs.springbootdev.core.config;


import cn.dev33.satoken.fun.strategy.SaCorsHandleFunction;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    /**
     * CORS 跨域处理策略
     */
    @Bean
    public SaCorsHandleFunction corsHandle() {
        return (req, res, sto) -> {
            String requestOrigin = req.getHeader("Origin");
            String responseOrigin = "*";
            
            if (requestOrigin != null && !requestOrigin.isBlank()) {
                if ("*".equals(allowedOrigins)) {
                    responseOrigin = requestOrigin;
                } else {
                    String[] origins = allowedOrigins.split(",");
                    for (String o : origins) {
                        if (o.trim().equalsIgnoreCase(requestOrigin.trim())) {
                            responseOrigin = requestOrigin;
                            break;
                        }
                    }
                }
            }

            res.setHeader("Access-Control-Allow-Origin", responseOrigin)                               // 允许指定域访问跨域资源
                    .setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT")// 允许的请求方式
                    .setHeader("Access-Control-Max-Age", "3600")                            // 有效时间
                    .setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, token")                        // 允许的header参数
                    .setHeader("Access-Control-Expose-Headers", "Content-Disposition") // 暴露 Content-Disposition 响应头
                    .setHeader("Access-Control-Allow-Credentials", "true"); // 允许跨域 Cookie/Credentials 共享
            SaRouter.match(SaHttpMethod.OPTIONS)
                    .back();
        };
    }
}
