package com.yeyamo_mobile.api.analytics_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
public class AnalyticsCacheConfig {
    @Bean
    RedisCacheManager analyticsCacheManager(RedisConnectionFactory connections,
            @Value("${analytics.cache.ttl-seconds:30}") long ttlSeconds) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(ttlSeconds))
            .disableCachingNullValues();
        return RedisCacheManager.builder(connections)
            .cacheDefaults(defaults)
            .transactionAware()
            .build();
    }
}
