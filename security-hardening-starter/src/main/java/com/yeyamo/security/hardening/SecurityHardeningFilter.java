package com.yeyamo.security.hardening;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class SecurityHardeningFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityHardeningFilter.class);
    private static final Pattern CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");
    private static final Pattern ENCODED_TRAVERSAL = Pattern.compile("(?i)(%2e|%2f|%5c|\\\\|(?:^|/)\\.\\.(?:/|$))");
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\x00-\\x1f\\x7f]");
    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
    private final HardeningProperties properties;
    private final InMemoryRateLimiter limiter;

    public SecurityHardeningFilter(HardeningProperties properties) {
        this.properties = properties;
        this.limiter = new InMemoryRateLimiter(properties.getMaxTrackedClients());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationId(request);
        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);
        addSecurityHeaders(request, response);
        try {
            String rejection = validateRequest(request);
            if (rejection != null) {
                reject(request, response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", rejection);
                return;
            }
            String origin = request.getHeader("Origin");
            if (origin != null && !allowedOrigin(origin)) {
                reject(request, response, HttpServletResponse.SC_FORBIDDEN, "ORIGIN_NOT_ALLOWED", "Origin is not allowed");
                return;
            }
            if (origin != null) {
                addCorsHeaders(response, origin);
            }
            if ("OPTIONS".equals(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            boolean authentication = isAuthenticationEndpoint(request.getRequestURI());
            int limit = authentication ? properties.getAuthenticationRequestsPerMinute() : properties.getRequestsPerMinute();
            int burst = authentication ? properties.getAuthenticationBurstCapacity() : properties.getBurstCapacity();
            String key = clientFingerprint(request) + ':' + (authentication ? "authentication" : "api");
            if (!limiter.tryAcquire(key, limit, burst)) {
                response.setHeader("Retry-After", "60");
                reject(request, response, 429, "RATE_LIMITED", "Too many requests");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String validateRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (uri == null || uri.length() > properties.getMaxUriLength()) {
            return "URI is too long";
        }
        if (query != null && query.length() > properties.getMaxQueryLength()) {
            return "Query string is too long";
        }
        if (CONTROL_CHARACTERS.matcher(uri).find() || (query != null && CONTROL_CHARACTERS.matcher(query).find())) {
            return "Control characters are not allowed";
        }
        if (ENCODED_TRAVERSAL.matcher(uri).find()) {
            return "Path traversal sequence detected";
        }
        if (!properties.getAllowedMethods().contains(request.getMethod())) {
            return "HTTP method is not allowed";
        }
        if (BODY_METHODS.contains(request.getMethod()) && request.getContentLengthLong() != 0) {
            String contentType = request.getContentType();
            if (contentType != null && !contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)
                    && !contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)
                    && !contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
                return "Unsupported content type";
            }
        }
        return null;
    }

    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        if (isApiDocumentation(request.getRequestURI())) {
            response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; object-src 'none'");
        } else {
            response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'; object-src 'none'");
        }
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        response.setHeader("Cross-Origin-Resource-Policy", "same-site");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        if (isSecure(request)) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if (isSensitivePath(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-store, max-age=0");
            response.setHeader("Pragma", "no-cache");
        }
    }

    private boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        return properties.isTrustForwardedProto()
                && "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private boolean allowedOrigin(String origin) {
        return properties.getAllowedOrigins().stream().anyMatch(origin::equals);
    }

    private void addCorsHeaders(HttpServletResponse response, String origin) {
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Allow-Methods", String.join(",", properties.getAllowedMethods()));
        response.setHeader("Access-Control-Allow-Headers", String.join(",", properties.getAllowedHeaders()));
        response.setHeader("Access-Control-Expose-Headers", "X-Correlation-Id,Retry-After,Location,ETag");
        response.setHeader("Access-Control-Max-Age", "600");
        if (properties.isAllowCredentials()) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
    }

    private String correlationId(HttpServletRequest request) {
        String value = request.getHeader("X-Correlation-Id");
        return value != null && CORRELATION_ID.matcher(value).matches() ? value : UUID.randomUUID().toString();
    }

    private String clientFingerprint(HttpServletRequest request) {
        String address = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private boolean isAuthenticationEndpoint(String uri) {
        String value = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        return value.contains("/auth/login") || value.contains("/auth/refresh")
                || value.contains("/password/") || value.contains("/verification/")
                || value.contains("/oauth/");
    }

    private boolean isSensitivePath(String uri) {
        String value = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        return value.contains("/auth/") || value.contains("/payment") || value.contains("/admin/");
    }

    private boolean isApiDocumentation(String uri) {
        String value = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        return value.contains("/swagger-ui") || value.contains("/v3/api-docs");
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status,
            String code, String message) throws IOException {
        LOGGER.warn("security_request_rejected code={} method={} path={} client={}", code,
                request.getMethod(), safePath(request.getRequestURI()), clientFingerprint(request));
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    private String safePath(String value) {
        if (value == null) {
            return "unknown";
        }
        return CONTROL_CHARACTERS.matcher(value).replaceAll("_").substring(0, Math.min(value.length(), 200));
    }
}
