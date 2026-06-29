package com.yeyamo_mobile.api.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

@Service
public class OtpService {

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

        String attemptsKey = attemptsKey(purpose, email);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, Duration.ofMinutes(expirationMinutes));

        if (attempts != null && attempts > maxAttempts) {
            delete(purpose, email);
            throw new ApiException("OTP_MAX_ATTEMPTS", "Nombre maximal de tentatives depasse", HttpStatus.TOO_MANY_REQUESTS);
        }

        String expectedHash = redisTemplate.opsForValue().get(otpKey(purpose, email));
        if (expectedHash == null) {
            throw new ApiException("OTP_EXPIRED", "Code OTP expire ou introuvable", HttpStatus.BAD_REQUEST);
        }

        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8), hash(otp).getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException("OTP_INVALID", "Code OTP invalide", HttpStatus.BAD_REQUEST);
        }

        delete(purpose, email);
    }

    public int expirationMinutes() {
        return expirationMinutes;
    }

    private void delete(String purpose, String email) {
        redisTemplate.delete(otpKey(purpose, email));
        redisTemplate.delete(attemptsKey(purpose, email));
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
