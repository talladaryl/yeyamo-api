package com.yeyamo_mobile.api.country_config_service.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for country data.
 * 
 * Cache TTLs:
 * - countries list: 1 hour (rarely changes)
 * - individual country: 1 hour
 * - country configuration: 1 hour
 * - country features: 30 minutes (may change more frequently)
 */
@Configuration
@Profile("!test")
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Country lists - 1 hour TTL
        cacheConfigurations.put("countries", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Individual country data - 1 hour TTL
        cacheConfigurations.put("country", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Full country configuration - 1 hour TTL
        cacheConfigurations.put("country-config", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Feature flags - 30 minutes TTL (may change more frequently)
        cacheConfigurations.put("country-features", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
