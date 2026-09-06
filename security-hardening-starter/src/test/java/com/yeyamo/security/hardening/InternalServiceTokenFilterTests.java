package com.yeyamo.security.hardening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class InternalServiceTokenFilterTests {

    private static final String SECRET = "super-secret-service-token-12345";

    private HardeningProperties propertiesWithToken(String token) {
        HardeningProperties properties = new HardeningProperties();
        properties.setInternalToken(token);
        return properties;
    }

    private void assertStandardErrorResponse(MockHttpServletResponse response, String expectedCorrelationId) throws Exception {
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String json = response.getContentAsString();
        assertTrue(json.contains("\"code\":\"INTERNAL_UNAUTHORIZED\""), "Must contain standard code INTERNAL_UNAUTHORIZED");
        assertTrue(json.contains("\"message\":\"Invalid or missing internal service token\""), "Must contain clear error message");
        assertTrue(json.contains("\"details\":[]"), "Must contain empty details array");
        assertTrue(json.contains("\"timestamp\":"), "Must contain timestamp field");
        if (expectedCorrelationId != null && !expectedCorrelationId.isBlank()) {
            assertTrue(json.contains("\"correlationId\":\"" + expectedCorrelationId + "\""), "Must propagate correlationId");
        }
    }

    @Test
    void allowsNonInternalRoutesWithoutToken() throws ServletException, IOException {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(propertiesWithToken(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/places");
        request.setRequestURI("/api/v1/places");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void rejectsInternalRouteWhenTokenHeaderMissing() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(propertiesWithToken(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/sync");
        request.setRequestURI("/internal/sync");
        request.addHeader("X-Correlation-Id", "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertStandardErrorResponse(response, "corr-123");
    }

    @Test
    void rejectsInternalRouteWhenTokenIsInvalid() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(propertiesWithToken(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/jobs/run");
        request.setRequestURI("/internal/jobs/run");
        request.addHeader("X-Internal-Token", "wrong-token-value");
        request.addHeader("X-Correlation-Id", "corr-456");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertStandardErrorResponse(response, "corr-456");
    }

    @Test
    void allowsInternalRouteWhenTokenIsValid() throws ServletException, IOException {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(propertiesWithToken(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/jobs/run");
        request.setRequestURI("/internal/jobs/run");
        request.addHeader("X-Internal-Token", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    @Test
    void failsClosedWhenTokenIsNotConfigured() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(propertiesWithToken(""));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/health");
        request.setRequestURI("/internal/health");
        request.addHeader("X-Internal-Token", "any-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertStandardErrorResponse(response, null);
    }

    @Test
    void protectsExactInternalPath() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(propertiesWithToken(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal");
        request.setRequestURI("/internal");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertStandardErrorResponse(response, null);
    }

    @Test
    void handlesContextPathCorrectly() throws ServletException, IOException {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(propertiesWithToken(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/status");
        request.setContextPath("/api");
        request.setRequestURI("/api/internal/status");
        request.addHeader("X-Internal-Token", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }
}
