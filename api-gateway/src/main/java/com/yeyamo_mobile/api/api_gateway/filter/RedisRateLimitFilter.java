package com.yeyamo_mobile.api.api_gateway.filter;

import java.io.IOException;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitFilter.class);
    private static final RedisScript<Long> SCRIPT = RedisScript.of(
            "local current = redis.call('INCR', KEYS[1]); "
                    + "if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return current;",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final boolean failOpen;
    private final long requests;
    private final long authenticationRequests;
    private final long uploadAudioRequests;
    private final long uploadArtworkRequests;
    private final long contributionRequests;
    private final long translationRequests;
    private final long quizAttemptRequests;
    private final long orderRequests;
    private final long reportingRequests;
    private final Duration window;

    public RedisRateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.fail-open:false}") boolean failOpen,
            @Value("${security.rate-limit.requests:120}") long requests,
            @Value("${security.rate-limit.authentication-requests:20}") long authenticationRequests,
            @Value("${security.rate-limit.culture.upload-audio.requests:10}") long uploadAudioRequests,
            @Value("${security.rate-limit.culture.upload-artwork.requests:20}") long uploadArtworkRequests,
            @Value("${security.rate-limit.culture.contributions.requests:30}") long contributionRequests,
            @Value("${security.rate-limit.culture.translations.requests:50}") long translationRequests,
            @Value("${security.rate-limit.culture.quiz-attempts.requests:20}") long quizAttemptRequests,
            @Value("${security.rate-limit.culture.orders.requests:30}") long orderRequests,
            @Value("${security.rate-limit.culture.reporting.requests:10}") long reportingRequests,
            @Value("${security.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.requests = requests;
        this.authenticationRequests = authenticationRequests;
        this.uploadAudioRequests = uploadAudioRequests;
        this.uploadArtworkRequests = uploadArtworkRequests;
        this.contributionRequests = contributionRequests;
        this.translationRequests = translationRequests;
        this.quizAttemptRequests = quizAttemptRequests;
        this.orderRequests = orderRequests;
        this.reportingRequests = reportingRequests;
        this.window = Duration.ofSeconds(windowSeconds);
        if (requests < 1 || authenticationRequests < 1 || uploadAudioRequests < 1 || uploadArtworkRequests < 1
                || contributionRequests < 1 || translationRequests < 1 || quizAttemptRequests < 1
                || orderRequests < 1 || reportingRequests < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Rate limit values must be positive");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled || request.getRequestURI().startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Limit limit = limitFor(request);
            Long current = redisTemplate.execute(
                    SCRIPT,
                    List.of("gateway:rate:" + limit.bucket() + ":" + clientHash(request)),
                    String.valueOf(window.toMillis()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit.requests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit.requests() - value(current))));
            if (value(current) > limit.requests()) {
                response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Trop de requêtes\"}");
                return;
            }
        } catch (DataAccessException exception) {
            log.warn("Redis rate limiter unavailable; failOpen={} cause={}", failOpen,
                    exception.getClass().getSimpleName());
            if (!failOpen) {
                response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Rate limiter unavailable");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private Limit limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/v1/auth/")) {
            return new Limit("auth", authenticationRequests);
        }
        if (isMutation(method) && (path.startsWith("/api/v1/moderation/reports")
                || (path.startsWith("/api/v1/interactions/") && path.endsWith("/REPORT")))) {
            return new Limit("culture-reporting", reportingRequests);
        }
        if (isMutation(method) && path.startsWith("/api/v1/artwork-orders")) {
            return new Limit("artwork-orders", orderRequests);
        }
        if (isMutation(method) && path.contains("/language-lessons/")
                && (path.endsWith("/attempts") || path.endsWith("/complete"))) {
            return new Limit("culture-quiz", quizAttemptRequests);
        }
        if (isMutation(method) && (path.startsWith("/api/v1/admin/culture/translations/")
                || (path.startsWith("/api/v1/culture/") && path.contains("/translations")))) {
            return new Limit("culture-translations", translationRequests);
        }
        if (isMutation(method) && path.startsWith("/api/v1/culture/contributions")) {
            return new Limit("culture-contributions", contributionRequests);
        }
        if (isArtworkUpload(request)) {
            return new Limit("artwork-upload", uploadArtworkRequests);
        }
        if (isAudioUpload(request)) {
            return new Limit("culture-audio-upload", uploadAudioRequests);
        }
        return new Limit("api", requests);
    }

    private boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    private boolean isArtworkUpload(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        String usageType = request.getParameter("usageType");
        return "/api/v1/artworks".equals(path)
                || ("/api/v1/media/culture".equals(path) && usageType != null && usageType.startsWith("ARTWORK_"));
    }

    private boolean isAudioUpload(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod()) || !"/api/v1/media/culture".equals(request.getRequestURI())) {
            return false;
        }
        String usageType = request.getParameter("usageType");
        return usageType != null && (usageType.endsWith("_AUDIO") || "LANGUAGE_PRONUNCIATION".equals(usageType));
    }

    private record Limit(String bucket, long requests) {
    }

    private String clientHash(HttpServletRequest request) {
        String address = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
