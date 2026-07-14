package com.yeyamo_mobile.api.interaction_service.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.yeyamo_mobile.api.interaction_service.application.InteractionSummary;

class RedisInteractionCacheAdapterTest {

    @Test
    void storesAndReadsCountersWithConfiguredTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        UUID postId = UUID.randomUUID();
        String key = "interaction:post:" + postId + ":counts";
        when(values.get(key)).thenReturn("7|5|2");
        RedisInteractionCacheAdapter adapter = new RedisInteractionCacheAdapter(redis, 45);

        Optional<InteractionSummary.Counts> result = adapter.getCounts(postId);
        adapter.putCounts(postId, result.orElseThrow());

        assertEquals(new InteractionSummary.Counts(7, 5, 2), result.orElseThrow());
        verify(values).set(key, "7|5|2", Duration.ofSeconds(45));
    }

    @Test
    void redisFailureDoesNotBreakTheDatabaseFallback() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("Redis unavailable"));

        RedisInteractionCacheAdapter adapter = new RedisInteractionCacheAdapter(redis, 60);

        assertTrue(adapter.getCounts(UUID.randomUUID()).isEmpty());
        assertDoesNotThrow(() -> adapter.evict(UUID.randomUUID()));
    }
}
