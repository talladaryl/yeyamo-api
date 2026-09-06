package com.yeyamo.security.hardening;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter protecting internal service-to-service endpoints (/internal/**)
 * using a shared pre-shared secret token passed in X-Internal-Token header.
 * Applies a strict fail-closed policy: if no token is configured, all /internal/**
 * requests are rejected with HTTP 401 formatted using the project's standard ErrorResponse.
 */
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalServiceTokenFilter.class);
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";
    private static final String INTERNAL_PATH_EXACT = "/internal";

    private final String configuredToken;
    private final ObjectMapper objectMapper;

    public InternalServiceTokenFilter(HardeningProperties properties) {
        this(properties, createDefaultObjectMapper());
    }

    public InternalServiceTokenFilter(String configuredToken) {
        this(configuredToken, createDefaultObjectMapper());
    }

    public InternalServiceTokenFilter(HardeningProperties properties, ObjectMapper objectMapper) {
        String token = properties != null ? properties.getInternalToken() : null;
        this.configuredToken = token != null ? token.trim() : "";
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
        if (this.configuredToken.isEmpty()) {
            LOGGER.warn("yeyamo.security.internal-token is not configured! All /internal/** endpoints will be blocked (fail-closed).");
        }
    }

    public InternalServiceTokenFilter(String configuredToken, ObjectMapper objectMapper) {
        this.configuredToken = configuredToken != null ? configuredToken.trim() : "";
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
        if (this.configuredToken.isEmpty()) {
            LOGGER.warn("yeyamo.security.internal-token is not configured! All /internal/** endpoints will be blocked (fail-closed).");
        }
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri;
        if (contextPath != null && !contextPath.isEmpty() && path != null && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (path == null || (!path.startsWith(INTERNAL_PATH_PREFIX) && !path.equals(INTERNAL_PATH_EXACT))) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (!isValidToken(providedToken)) {
            LOGGER.warn("unauthorized_internal_request path={} client={}", path, request.getRemoteAddr());
            rejectUnauthorized(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String providedToken) {
        if (configuredToken.isEmpty() || providedToken == null || providedToken.isBlank()) {
            return false;
        }
        byte[] expected = configuredToken.getBytes(StandardCharsets.UTF_8);
        byte[] actual = providedToken.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private void rejectUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader("X-Correlation-Id");
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader("X-Correlation-ID");
        }
        if (correlationId == null) {
            correlationId = "";
        }

        ErrorResponse errorResponse = ErrorResponse.of(
                "INTERNAL_UNAUTHORIZED",
                "Invalid or missing internal service token",
                correlationId
        );

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
