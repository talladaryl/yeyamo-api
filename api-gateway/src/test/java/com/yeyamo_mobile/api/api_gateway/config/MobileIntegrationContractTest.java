package com.yeyamo_mobile.api.api_gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class MobileIntegrationContractTest {
    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    @Test
    void servesAValidVersionedMobileOpenApiIndex() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/static/mobile-api/openapi.json")) {
            assertNotNull(input);
            JsonNode specification = new ObjectMapper().readTree(input);
            assertEquals("3.1.0", specification.path("openapi").asText());
            assertTrue(specification.path("paths").has("/api/v1/auth/login"));
            assertTrue(specification.path("paths").has("/api/v1/collections/{id}"));
            long operations = specification.path("paths").properties().stream()
                    .flatMap(entry -> entry.getValue().properties().stream())
                    .filter(entry -> HTTP_METHODS.contains(entry.getKey()))
                    .count();
            assertTrue(operations >= 59,
                    () -> "The versioned mobile OpenAPI index must retain at least the 59 baseline operations; found "
                            + operations);
        }
    }

    @Test
    void allowsExpoWebLocalOriginsAndHeaders() {
        SecurityConfig security = new SecurityConfig();
        var source = security.corsConfigurationSource(
                "http://localhost:*,http://127.0.0.1:*,http://10.0.2.2:*,http://192.168.*:*");
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals("http://localhost:8081",
                configuration.checkOrigin("http://localhost:8081"));
        assertEquals("http://192.168.1.20:8081",
                configuration.checkOrigin("http://192.168.1.20:8081"));
        assertTrue(configuration.getAllowedHeaders().contains("X-Requested-With"));
    }

    @Test
    void exposesOnlyTheExpectedAnonymousMobileRoutes() {
        assertTrue(SecurityConfig.isPublicMobileRequest(request("POST", "/api/v1/auth/register")));
        assertTrue(SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/regions")));
        assertTrue(SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/events/upcoming")));
        assertTrue(SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/catalog/assets")));
        assertTrue(SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/culture/contents")));
        assertTrue(SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/culture-graph/discover")));
        assertTrue(SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/artworks")));
        assertTrue(SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/artisans")));

        assertTrue(!SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/auth/me")));
        assertTrue(!SecurityConfig.isPublicMobileRequest(request("POST", "/api/v1/auth/logout")));
        assertTrue(!SecurityConfig.isPublicMobileRequest(request("GET", "/api/v1/events/me")));
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
