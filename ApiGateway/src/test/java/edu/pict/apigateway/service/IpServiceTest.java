package edu.pict.apigateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

class IpServiceTest {

    private IpService ipService;

    @BeforeEach
    void setUp() {
        ipService = new IpService();
    }

    @Test
    void testResolveClientIp_XForwardedFor() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "192.168.1.1, 10.0.0.1");

        String resolvedIp = ipService.resolveClientIp(headers, "127.0.0.1");
        assertEquals("192.168.1.1", resolvedIp);
    }

    @Test
    void testResolveClientIp_XRealIp() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Real-IP", "172.16.0.1");

        String resolvedIp = ipService.resolveClientIp(headers, "127.0.0.1");
        assertEquals("172.16.0.1", resolvedIp);
    }

    @Test
    void testResolveClientIp_RemoteAddressFallback() {
        HttpHeaders headers = new HttpHeaders();

        String resolvedIp = ipService.resolveClientIp(headers, "203.0.113.5");
        assertEquals("203.0.113.5", resolvedIp);
    }

    @Test
    void testResolveClientIp_InvalidIpInHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "invalid-ip");
        headers.add("X-Real-IP", "999.999.999.999");

        // Should fallback to remote address if remote address is valid
        String resolvedIp = ipService.resolveClientIp(headers, "8.8.8.8");
        assertEquals("8.8.8.8", resolvedIp);
    }

    @Test
    void testResolveClientIp_AllInvalid() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "invalid");

        String resolvedIp = ipService.resolveClientIp(headers, "not-an-ip");
        assertNull(resolvedIp);
    }
}
