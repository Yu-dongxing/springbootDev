/*
 * Copyright 漏 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.core.utils;

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
    private static final java.util.regex.Pattern IPV4_PATTERN =
            java.util.regex.Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    private static final java.util.regex.Pattern IPV6_PATTERN =
            java.util.regex.Pattern.compile("^([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}$|" +
                    "^([0-9a-fA-F]{1,4}:){1,7}:$|" +
                    "^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
                    "^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|" +
                    "^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|" +
                    "^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|" +
                    "^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|" +
                    "^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$|" +
                    "^:(:[0-9a-fA-F]{1,4}){1,7}$|" +
                    "^::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])$|" +
                    "^([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])$");

    public static boolean isValidIp(String ip) {
        if (ip == null) {
            return false;
        }
        String trimmed = ip.trim();
        return IPV4_PATTERN.matcher(trimmed).matches() || IPV6_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isInternalIp(String ip) {
        if (ip == null) {
            return false;
        }
        String trimmed = ip.trim();
        if ("127.0.0.1".equals(trimmed) || "0:0:0:0:0:0:0:1".equals(trimmed) || "::1".equals(trimmed)) {
            return true;
        }
        if (trimmed.startsWith("10.") || trimmed.startsWith("192.168.")) {
            return true;
        }
        if (trimmed.startsWith("172.")) {
            String[] parts = trimmed.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {}
            }
        }
        return false;
    }

    /**
     * 获取客户端真实 IP 及来源信息，便于排查代理链问题。
     */
    public static IpResolveResult resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        
        // 只有当请求的直连源地址（RemoteAddr）为本地或内网 IP 时，我们才相信代理请求头，否则视为公网客户端直连，忽略所有代理头
        if (isInternalIp(remoteAddr)) {
            for (String header : IP_HEADER_CANDIDATES) {
                String headerValue = request.getHeader(header);
                String candidateIp = extractClientIp(header, headerValue);
                if (candidateIp != null) {
                    return new IpResolveResult(candidateIp, header, headerValue, remoteAddr);
                }
            }
        }

        return new IpResolveResult(normalizeLoopbackIp(remoteAddr), "RemoteAddr",
                remoteAddr, remoteAddr);
    }

    /**
     * 输出常见代理头，便于开发联调时快速定位取值来源。
     */
    public static Map<String, String> collectIpHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String header : IP_HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value.trim())) {
                headers.put(header, value);
            }
        }
        headers.put("RemoteAddr", request.getRemoteAddr());
        return headers;
    }

    private static String extractClientIp(String headerName, String rawValue) {
        if (rawValue == null || rawValue.isBlank() || UNKNOWN.equalsIgnoreCase(rawValue.trim())) {
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
        if (rawValue == null || rawValue.isBlank() || UNKNOWN.equalsIgnoreCase(rawValue.trim())) {
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
        if (value.isBlank() || UNKNOWN.equalsIgnoreCase(value) || "_hidden".equalsIgnoreCase(value) || !isValidIp(value)) {
            return null;
        }

        return normalizeLoopbackIp(value);
    }

    private static boolean isValidIpToken(String value) {
        return value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value.trim()) && isValidIp(value.trim());
    }

    private static String normalizeLoopbackIp(String ip) {
        if (ip == null || ip.isBlank() || UNKNOWN.equalsIgnoreCase(ip.trim())) {
            return ip;
        }

        String trimmed = ip.trim();
        if ("127.0.0.1".equals(trimmed) || "0:0:0:0:0:0:0:1".equals(trimmed) || "::1".equals(trimmed)) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ignored) {
                return "127.0.0.1";
            }
        }
        return trimmed;
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
