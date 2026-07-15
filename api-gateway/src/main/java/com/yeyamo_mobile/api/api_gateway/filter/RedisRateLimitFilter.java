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
    private final Duration window;

    public RedisRateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.fail-open:false}") boolean failOpen,
            @Value("${security.rate-limit.requests:120}") long requests,
            @Value("${security.rate-limit.authentication-requests:20}") long authenticationRequests,
            @Value("${security.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.requests = requests;
        this.authenticationRequests = authenticationRequests;
        this.window = Duration.ofSeconds(windowSeconds);
        if (requests < 1 || authenticationRequests < 1 || windowSeconds < 1) {
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
            boolean authentication = request.getRequestURI().startsWith("/api/v1/auth/");
            long requestLimit = authentication ? authenticationRequests : requests;
            Long current = redisTemplate.execute(
                    SCRIPT,
                    List.of("gateway:rate:" + (authentication ? "auth:" : "api:") + clientHash(request)),
                    String.valueOf(window.toMillis()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(requestLimit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, requestLimit - value(current))));
            if (value(current) > requestLimit) {
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
