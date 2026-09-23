package com.yeyamo_mobile.api.feed_service.infrastructure.cache;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.feed_service.application.FeedPage;
import com.yeyamo_mobile.api.feed_service.application.port.FeedCachePort;

@Component
public class RedisFeedCacheAdapter implements FeedCachePort {
    private static final Logger log = LoggerFactory.getLogger(RedisFeedCacheAdapter.class);
    private static final String CACHE_PREFIX = "feed:v2";
    private static final String VERSION = CACHE_PREFIX + ":version";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public RedisFeedCacheAdapter(
            StringRedisTemplate redis,
            ObjectMapper mapper,
            @Value("${feed.cache.ttl-seconds:60}") long seconds) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = Duration.ofSeconds(seconds);
    }

    public Optional<FeedPage> get(String user, int page, int size) {
        try {
            String value = redis.opsForValue().get(key(user, page, size));
            return value == null ? Optional.empty() : Optional.of(mapper.readValue(value, FeedPage.class));
        } catch (Exception exception) {
            log.debug("Feed cache read unavailable: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public void put(FeedPage page) {
        try {
            redis.opsForValue().set(
                    key(page.userId(), page.page(), page.size()),
                    mapper.writeValueAsString(page),
                    ttl);
        } catch (Exception exception) {
            log.debug("Feed cache write unavailable: {}", exception.getMessage());
        }
    }

    public void invalidate() {
        try {
            redis.opsForValue().increment(VERSION);
        } catch (RuntimeException exception) {
            log.debug("Feed cache invalidation unavailable: {}", exception.getMessage());
        }
    }

    private String key(String user, int page, int size) {
        String version = redis.opsForValue().get(VERSION);
        return CACHE_PREFIX + ":" + (version == null ? "0" : version) + ":" + user + ":" + page + ":" + size;
    }
}
