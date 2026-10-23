package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

class OtpServiceAtomicTests {

    @Test
    void successfulScriptResultConsumesOtp() {
        StringRedisTemplate redis = redisReturning(1L);

        assertDoesNotThrow(() -> new OtpService(redis, 10, 5).verify("email", "user@example.com", "123456"));
    }

    @Test
    void invalidAndExpiredScriptResultsRemainControlledErrors() {
        assertThrows(ApiException.class,
                () -> new OtpService(redisReturning(-1L), 10, 5)
                        .verify("email", "user@example.com", "123456"));
        assertThrows(ApiException.class,
                () -> new OtpService(redisReturning(0L), 10, 5)
                        .verify("email", "user@example.com", "123456"));
        assertThrows(ApiException.class,
                () -> new OtpService(redisReturning(2L), 10, 5)
                        .verify("email", "user@example.com", "123456"));
    }

    private StringRedisTemplate redisReturning(Long result) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doReturn(result).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        return redis;
    }
}
