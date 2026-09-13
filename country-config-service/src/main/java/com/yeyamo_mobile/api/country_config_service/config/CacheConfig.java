package com.yeyamo_mobile.api.country_config_service.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    /**
     * The Redis serializer owns a separate Jackson ObjectMapper from Spring MVC.
     * Register Java Time support here so cached DTOs containing Instant (and the
     * other java.time types supported by JavaTimeModule) round-trip correctly.
     *
     * The serializer's existing default typing behaviour is deliberately left
     * unchanged; this only adds date/time codecs to the ObjectMapper it creates.
     */
    static RedisSerializer<Object> redisValueSerializer() {
        GenericJackson2JsonRedisSerializer delegate = new GenericJackson2JsonRedisSerializer()
                .configure(objectMapper -> objectMapper.registerModule(new JavaTimeModule()));

        return new RedisSerializer<>() {
            @Override
            public byte[] serialize(Object value) {
                return delegate.serialize(normalizeRootCollection(value));
            }

            @Override
            public Object deserialize(byte[] bytes) {
                return delegate.deserialize(bytes);
            }
        };
    }

    /**
     * {@code Stream.toList()} returns an immutable JDK collection that Jackson's
     * generic type resolver does not tag at the root. Redis caches the returned
     * object as {@code Object}, so normalize root collections to concrete,
     * serializable JDK types before delegating. Their elements and ordering are
     * preserved, and deserialization returns standard concrete collections with
     * equivalent contents.
     */
    private static Object normalizeRootCollection(Object value) {
        if (value instanceof java.util.List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Set<?> set) {
            return new LinkedHashSet<>(set);
        }
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>(map);
        }
        return value;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = defaultCacheConfiguration();

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

    static RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer()))
                .disableCachingNullValues();
    }
}
