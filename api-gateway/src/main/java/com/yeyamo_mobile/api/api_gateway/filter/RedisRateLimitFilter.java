package com.yeyamo_mobile.api.api_gateway.filter;

import java.io.IOException;
import java.time.Duration;
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
    private final Duration window;

    public RedisRateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${security.rate-limit.enabled:true}") boolean enabled,
            @Value("${security.rate-limit.fail-open:true}") boolean failOpen,
            @Value("${security.rate-limit.requests:120}") long requests,
            @Value("${security.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.requests = requests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled || request.getRequestURI().startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Long current = redisTemplate.execute(
                    SCRIPT,
                    List.of("gateway:rate:" + request.getRemoteAddr()),
                    String.valueOf(window.toMillis()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(requests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, requests - value(current))));
            if (value(current) > requests) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Trop de requêtes\"}");
                return;
            }
        } catch (DataAccessException exception) {
            log.warn("Redis rate limiter unavailable; failOpen={}", failOpen, exception);
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
}
