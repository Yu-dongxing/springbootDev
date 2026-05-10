/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.modules.system.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.utils.IpUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公共接口 -- 客户端IP获取
 * 客户端IP获取/代理链查询
 * @author YuDongXing
 * @since 2026/04/16
 */
@Slf4j
@RestController
@RequestMapping("/api/common")
public class IpController {
    /**
     * 获取客户端真实 IP
     * @author YuDongXing
     * @since 2026/04/16
     */
    @GetMapping("/client-ip")
    public Result<Map<String, Object>> getClientIp(HttpServletRequest request) {
        IpUtils.IpResolveResult ipResolveResult = IpUtils.resolveClientIp(request);
        Map<String, Object> data = new LinkedHashMap<>(ipResolveResult.toMap());
        data.put("headers", IpUtils.collectIpHeaders(request));
        return Result.success(data);
    }
}
