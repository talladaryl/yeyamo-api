package com.yeyamo_mobile.api.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

@Service
public class OtpService {

    private static final DefaultRedisScript<Long> VERIFY_AND_CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local attempts = redis.call('INCR', KEYS[2])
            redis.call('EXPIRE', KEYS[2], ARGV[1])
            if attempts > tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1], KEYS[2])
                return 2
            end
            local expected = redis.call('GET', KEYS[1])
            if not expected then
                return 0
            end
            if expected ~= ARGV[3] then
                return -1
            end
            redis.call('DEL', KEYS[1], KEYS[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int expirationMinutes;
    private final int maxAttempts;

    public OtpService(
            StringRedisTemplate redisTemplate,
            @Value("${otp.expiration-minutes}") int expirationMinutes,
            @Value("${otp.max-attempts}") int maxAttempts
    ) {
        this.redisTemplate = redisTemplate;
        this.expirationMinutes = expirationMinutes;
        this.maxAttempts = maxAttempts;
    }

    public String generate(String purpose, String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        redisTemplate.opsForValue().set(otpKey(purpose, email), hash(otp), Duration.ofMinutes(expirationMinutes));
        redisTemplate.delete(attemptsKey(purpose, email));
        return otp;
    }

    public void verify(String purpose, String email, String otp) {
        if (otp == null || otp.isBlank()) {
            throw new ApiException("OTP_REQUIRED", "Code OTP requis", HttpStatus.BAD_REQUEST);
        }

        long expirationSeconds = Math.max(1, Duration.ofMinutes(expirationMinutes).getSeconds());
        Long result = redisTemplate.execute(
                VERIFY_AND_CONSUME_SCRIPT,
                java.util.List.of(otpKey(purpose, email), attemptsKey(purpose, email)),
                String.valueOf(expirationSeconds),
                String.valueOf(maxAttempts),
                hash(otp));

        if (Long.valueOf(2L).equals(result)) {
            throw new ApiException("OTP_MAX_ATTEMPTS", "Nombre maximal de tentatives depasse", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (Long.valueOf(0L).equals(result)) {
            throw new ApiException("OTP_EXPIRED", "Code OTP expire ou introuvable", HttpStatus.BAD_REQUEST);
        }
        if (!Long.valueOf(1L).equals(result)) {
            throw new ApiException("OTP_INVALID", "Code OTP invalide", HttpStatus.BAD_REQUEST);
        }
    }

    public int expirationMinutes() {
        return expirationMinutes;
    }

    private String otpKey(String purpose, String email) {
        return "auth:otp:" + purpose + ":" + normalize(email);
    }

    private String attemptsKey(String purpose, String email) {
        return "auth:otp:attempts:" + purpose + ":" + normalize(email);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible", exception);
        }
    }
}
