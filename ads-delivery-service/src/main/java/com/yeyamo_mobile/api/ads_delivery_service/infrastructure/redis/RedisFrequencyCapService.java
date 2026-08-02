package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.redis;

import com.yeyamo_mobile.api.ads_delivery_service.domain.port.FrequencyCapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisFrequencyCapService implements FrequencyCapService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${yeyamo.ads.frequency-cap.default-daily-limit:5}")
    private int dailyLimit;

    @Value("${yeyamo.ads.frequency-cap.default-hourly-limit:2}")
    private int hourlyLimit;

    public RedisFrequencyCapService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean canShowAd(String userId, String campaignId, String timeWindow) {
        if (userId == null) {
            return true; // No frequency cap for anonymous users
        }

        String key = buildKey(userId, campaignId, timeWindow);
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        int limit = "24h".equals(timeWindow) ? dailyLimit : hourlyLimit;
        return count < limit;
    }

    @Override
    public void recordImpression(String userId, String campaignId) {
        if (userId == null) {
            return;
        }

        // Record for 24h window
        String dailyKey = buildKey(userId, campaignId, "24h");
        redisTemplate.opsForValue().increment(dailyKey);
        redisTemplate.expire(dailyKey, Duration.ofHours(24));

        // Record for 1h window
        String hourlyKey = buildKey(userId, campaignId, "1h");
        redisTemplate.opsForValue().increment(hourlyKey);
        redisTemplate.expire(hourlyKey, Duration.ofHours(1));
    }

    private String buildKey(String userId, String campaignId, String timeWindow) {
        return String.format("freq:cap:%s:%s:%s", userId, campaignId, timeWindow);
    }
}
