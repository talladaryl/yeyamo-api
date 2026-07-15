package com.yeyamo.security.hardening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHardeningFilterTests {

    @Test
    void addsSecurityHeadersAndCorsForAnExplicitlyAllowedOrigin() throws Exception {
        HardeningProperties properties = properties();
        SecurityHardeningFilter filter = new SecurityHardeningFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog");
        request.addHeader("Origin", "https://app.yeyamo.example");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("https://app.yeyamo.example", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("max-age=31536000; includeSubDomains", response.getHeader("Strict-Transport-Security"));
    }

    @Test
    void doesNotEmitHstsOnAnUntrustedPlainHttpRequest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SecurityHardeningFilter(properties()).doFilter(
                new MockHttpServletRequest("GET", "/actuator/health"), response, new MockFilterChain());

        assertNull(response.getHeader("Strict-Transport-Security"));
    }

    @Test
    void rejectsPathTraversalBeforeTheController() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/media/%2e%2e/secrets");
        request.setRequestURI("/api/v1/media/%2e%2e/secrets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SecurityHardeningFilter(properties()).doFilter(request, response, new MockFilterChain());

        assertEquals(400, response.getStatus());
        assertEquals(true, response.getContentAsString().contains("INVALID_REQUEST"));
    }

    @Test
    void rejectsAnUnlistedBrowserOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent("{}".getBytes());
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SecurityHardeningFilter(properties()).doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void rateLimitsAuthenticationBurstsWithoutLeakingClientAddress() throws Exception {
        HardeningProperties properties = properties();
        properties.setAuthenticationRequestsPerMinute(1);
        properties.setAuthenticationBurstCapacity(1);
        SecurityHardeningFilter filter = new SecurityHardeningFilter(properties);

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        first.setContentType("application/json");
        first.setContent("{}".getBytes());
        first.setRemoteAddr("203.0.113.42");
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        second.setContentType("application/json");
        second.setContent("{}".getBytes());
        second.setRemoteAddr("203.0.113.42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(second, response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertEquals(false, response.getContentAsString().contains("203.0.113.42"));
    }

    private HardeningProperties properties() {
        HardeningProperties properties = new HardeningProperties();
        properties.setAllowedOrigins(List.of("https://app.yeyamo.example"));
        return properties;
    }
}
