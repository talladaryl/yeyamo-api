package com.yeyamo_mobile.api.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

@Service
public class LoginAttemptService {
    private final StringRedisTemplate redisTemplate;
    private final int maximumAttempts;
    private final int turnstileThreshold;
    private final Duration attemptWindow;
    private final Duration lockDuration;

    public LoginAttemptService(StringRedisTemplate redisTemplate,
            @Value("${security.max-login-attempts:5}") int maximumAttempts,
            @Value("${security.login-turnstile-threshold:3}") int turnstileThreshold,
            @Value("${auth.login.attempt-window:15m}") Duration attemptWindow,
            @Value("${auth.login.lock-duration:15m}") Duration lockDuration) {
        this.redisTemplate = redisTemplate;
        this.maximumAttempts = maximumAttempts;
        this.turnstileThreshold = turnstileThreshold;
        this.attemptWindow = attemptWindow;
        this.lockDuration = lockDuration;
        if (maximumAttempts < 1 || turnstileThreshold < 1 || turnstileThreshold > maximumAttempts
                || attemptWindow.isNegative() || attemptWindow.isZero()
                || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("Invalid login protection configuration");
        }
    }

    public void assertAllowed(String identifier) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(identifier)))) {
            throw new ApiException("LOGIN_TEMPORARILY_LOCKED",
                    "Trop de tentatives. Reessayez plus tard.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public boolean requiresTurnstile(String identifier) {
        String value = redisTemplate.opsForValue().get(attemptsKey(identifier));
        if (value == null) return false;
        try {
            return Long.parseLong(value) >= turnstileThreshold;
        } catch (NumberFormatException invalidState) {
            redisTemplate.delete(attemptsKey(identifier));
            return false;
        }
    }

    public void failed(String identifier) {
        String attemptsKey = attemptsKey(identifier);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, attemptWindow);
        }
        if (attempts != null && attempts >= maximumAttempts) {
            redisTemplate.opsForValue().set(lockKey(identifier), "1", lockDuration);
            redisTemplate.delete(attemptsKey);
        }
    }

    public void succeeded(String identifier) {
        redisTemplate.delete(attemptsKey(identifier));
        redisTemplate.delete(lockKey(identifier));
    }

    private String attemptsKey(String identifier) {
        return "auth:login:attempts:" + digest(identifier);
    }

    private String lockKey(String identifier) {
        return "auth:login:lock:" + digest(identifier);
    }

    private String digest(String identifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(identifier.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
