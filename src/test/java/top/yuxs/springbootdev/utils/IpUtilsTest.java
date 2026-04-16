package top.yuxs.springbootdev.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class IpUtilsTest {

    @Test
    void shouldUseRemoteAddrWhenNoProxyHeaderExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");

        String clientIp = IpUtils.getClientIp(request);

        Assertions.assertEquals("192.168.1.10", clientIp);
    }

    @Test
    void shouldResolveFirstIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.8, 10.0.0.2, 10.0.0.3");

        IpUtils.IpResolveResult result = IpUtils.resolveClientIp(request);

        Assertions.assertEquals("203.0.113.8", result.getClientIp());
        Assertions.assertEquals("X-Forwarded-For", result.getSource());
    }

    @Test
    void shouldResolveIpFromForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("Forwarded", "for=198.51.100.7;proto=https;by=203.0.113.43");

        IpUtils.IpResolveResult result = IpUtils.resolveClientIp(request);

        Assertions.assertEquals("198.51.100.7", result.getClientIp());
        Assertions.assertEquals("Forwarded", result.getSource());
    }

    @Test
    void shouldIgnoreUnknownValueAndFallbackToNextHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "198.51.100.9");

        IpUtils.IpResolveResult result = IpUtils.resolveClientIp(request);

        Assertions.assertEquals("198.51.100.9", result.getClientIp());
        Assertions.assertEquals("X-Real-IP", result.getSource());
    }
}
