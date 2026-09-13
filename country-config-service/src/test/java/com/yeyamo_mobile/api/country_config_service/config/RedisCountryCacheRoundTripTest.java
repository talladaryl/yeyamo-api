package com.yeyamo_mobile.api.country_config_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLaunchStatus;
import com.yeyamo_mobile.api.country_config_service.dto.CountryDto;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

class RedisCountryCacheRoundTripTest {

    @Test
    void writesCountryListToRedisThenReadsItBackWithoutRequeryingTheSource() {
        Map<String, byte[]> redisValues = new HashMap<>();
        RedisCacheWriter writer = mock(RedisCacheWriter.class);
        when(writer.get(anyString(), any(byte[].class))).thenAnswer(invocation ->
                redisValues.get(storageKey(invocation.getArgument(0), invocation.getArgument(1))));
        doAnswer(invocation -> {
            redisValues.put(storageKey(invocation.getArgument(0), invocation.getArgument(1)), invocation.getArgument(2));
            return null;
        }).when(writer).put(anyString(), any(byte[].class), any(byte[].class), any(Duration.class));

        RedisCacheConfiguration configuration = CacheConfig.defaultCacheConfiguration();
        RedisCache cache = new TestRedisCache("countries", writer, configuration);
        AtomicInteger sourceCalls = new AtomicInteger();
        List<CountryDto> databaseResult = List.of(countryDto());

        List<CountryDto> firstRead = loadFromSource(sourceCalls, databaseResult);
        cache.put("all", firstRead);
        List<?> cachedRead = (List<?>) cache.get("all").get();

        assertThat(sourceCalls).hasValue(1);
        assertThat(redisValues).isNotEmpty();
        assertThat(firstRead).hasSize(1);
        assertThat(cachedRead).hasSize(1);
        assertThat(firstRead.getFirst()).isEqualTo(databaseResult.getFirst());
        assertThat(cachedRead.getFirst()).isEqualTo(databaseResult.getFirst());
        assertThat(((CountryDto) cachedRead.getFirst()).createdAt()).isEqualTo(databaseResult.getFirst().createdAt());
    }

    private static List<CountryDto> loadFromSource(AtomicInteger sourceCalls, List<CountryDto> databaseResult) {
        sourceCalls.incrementAndGet();
        return databaseResult;
    }

    private static String storageKey(String cacheName, byte[] key) {
        return cacheName + ':' + new String(key, StandardCharsets.UTF_8);
    }

    private static CountryDto countryDto() {
        Instant createdAt = Instant.parse("2026-09-13T12:00:00Z");
        return new CountryDto(
                UUID.fromString("e0186c42-b09f-4d91-a6fd-d5e64f50f15e"),
                "CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237", CountryLaunchStatus.LIVE,
                true, true, true, true, true, true, true, true, true, true,
                createdAt, createdAt.plusSeconds(60));
    }

    private static final class TestRedisCache extends RedisCache {
        private TestRedisCache(String name, RedisCacheWriter writer, RedisCacheConfiguration configuration) {
            super(name, writer, configuration);
        }
    }
}
