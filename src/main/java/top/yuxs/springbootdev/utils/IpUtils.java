/*
 * Copyright 漏 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端 IP 获取工具。
 *
 * <p>支持以下场景：
 * 1. 直连请求，回退到 {@link HttpServletRequest#getRemoteAddr()}
 * 2. Nginx / Apache / 网关代理，优先解析 X-Forwarded-For、X-Real-IP 等头
 * 3. 标准 Forwarded 头（RFC 7239）
 * 4. 多级代理场景，自动提取代理链中的首个有效真实 IP
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Forwarded",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_REAL_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED",
            "X-Cluster-Client-IP",
            "True-Client-IP",
            "CF-Connecting-IP",
            "Fastly-Client-IP",
            "Ali-CDN-Real-IP",
            "X-Original-Forwarded-For",
            "X-Original-Remote-Addr"
    };

    private IpUtils() {
    }

    /**
     * 获取客户端真实 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        IpResolveResult result = resolveClientIp(request);
        return result.getClientIp();
    }

    /**
     * 获取客户端真实 IP 及来源信息，便于排查代理链问题。
     */
    public static IpResolveResult resolveClientIp(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String headerValue = request.getHeader(header);
            String candidateIp = extractClientIp(header, headerValue);
            if (candidateIp != null) {
                return new IpResolveResult(candidateIp, header, headerValue, request.getRemoteAddr());
            }
        }

        return new IpResolveResult(normalizeLoopbackIp(request.getRemoteAddr()), "RemoteAddr",
                request.getRemoteAddr(), request.getRemoteAddr());
    }

    /**
     * 输出常见代理头，便于开发联调时快速定位取值来源。
     */
    public static Map<String, String> collectIpHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String header : IP_HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (isValidIpToken(value)) {
                headers.put(header, value);
            }
        }
        headers.put("RemoteAddr", request.getRemoteAddr());
        return headers;
    }

    private static String extractClientIp(String headerName, String rawValue) {
        if (!isValidIpToken(rawValue)) {
            return null;
        }

        if ("Forwarded".equalsIgnoreCase(headerName) || "HTTP_FORWARDED".equalsIgnoreCase(headerName)) {
            return parseForwardedHeader(rawValue);
        }

        return parseIpList(rawValue);
    }

    private static String parseForwardedHeader(String rawValue) {
        String[] segments = rawValue.split(",");
        for (String segment : segments) {
            String[] parts = segment.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.regionMatches(true, 0, "for=", 0, 4)) {
                    continue;
                }
                String value = trimmed.substring(4).trim();
                String ip = sanitizeIpToken(value);
                if (ip != null) {
                    return ip;
                }
            }
        }
        return null;
    }

    private static String parseIpList(String rawValue) {
        String[] parts = rawValue.split(",");
        for (String part : parts) {
            String ip = sanitizeIpToken(part);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String sanitizeIpToken(String rawValue) {
        if (!isValidIpToken(rawValue)) {
            return null;
        }

        String value = rawValue.trim().replace("\"", "");
        if (value.startsWith("for=")) {
            value = value.substring(4).trim();
        }

        if (value.startsWith("[")) {
            int endIndex = value.indexOf(']');
            if (endIndex > 0) {
                value = value.substring(1, endIndex);
            }
        } else if (value.contains(":") && value.indexOf(':') == value.lastIndexOf(':')) {
            int colonIndex = value.indexOf(':');
            if (colonIndex > 0 && value.substring(0, colonIndex).contains(".")) {
                value = value.substring(0, colonIndex);
            }
        }

        value = value.trim();
        if (!isValidIpToken(value) || "_hidden".equalsIgnoreCase(value)) {
            return null;
        }

        return normalizeLoopbackIp(value);
    }

    private static boolean isValidIpToken(String value) {
        return value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value.trim());
    }

    private static String normalizeLoopbackIp(String ip) {
        if (!isValidIpToken(ip)) {
            return ip;
        }

        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ignored) {
                return "127.0.0.1";
            }
        }
        return ip;
    }

    public static final class IpResolveResult {
        private final String clientIp;
        private final String source;
        private final String sourceValue;
        private final String remoteAddr;

        public IpResolveResult(String clientIp, String source, String sourceValue, String remoteAddr) {
            this.clientIp = clientIp;
            this.source = source;
            this.sourceValue = sourceValue;
            this.remoteAddr = remoteAddr;
        }

        public String getClientIp() {
            return clientIp;
        }

        public String getSource() {
            return source;
        }

        public String getSourceValue() {
            return sourceValue;
        }

        public String getRemoteAddr() {
            return remoteAddr;
        }

        public Map<String, String> toMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("clientIp", clientIp);
            map.put("source", source);
            map.put("sourceValue", sourceValue);
            map.put("remoteAddr", remoteAddr);
            return map;
        }
    }
}
