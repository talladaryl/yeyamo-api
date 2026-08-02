package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.redis;

import com.yeyamo_mobile.api.ads_delivery_service.domain.port.BudgetReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class RedisBudgetReservationService implements BudgetReservationService {

    private static final Logger logger = LoggerFactory.getLogger(RedisBudgetReservationService.class);

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${yeyamo.ads.budget-reservation.ttl-seconds:60}")
    private long reservationTtlSeconds;

    public RedisBudgetReservationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean reserveBudget(String campaignId, BigDecimal amount, String reservationId) {
        String key = buildReservationKey(reservationId);
        String value = String.format("%s:%s", campaignId, amount.toString());
        
        // Set with TTL
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(reservationTtlSeconds));
        
        if (Boolean.TRUE.equals(success)) {
            logger.debug("Reserved budget {} for campaign {} with reservation {}", amount, campaignId, reservationId);
        } else {
            logger.warn("Failed to reserve budget for reservation {}", reservationId);
        }
        
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void confirmReservation(String reservationId, BigDecimal actualAmount) {
        String key = buildReservationKey(reservationId);
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            String[] parts = value.split(":");
            String campaignId = parts[0];
            
            // Delete reservation
            redisTemplate.delete(key);
            
            logger.debug("Confirmed reservation {} for campaign {} with actual amount {}", 
                reservationId, campaignId, actualAmount);
        }
    }

    @Override
    public void releaseReservation(String reservationId) {
        String key = buildReservationKey(reservationId);
        redisTemplate.delete(key);
        
        logger.debug("Released reservation {}", reservationId);
    }

    private String buildReservationKey(String reservationId) {
        return String.format("budget:reservation:%s", reservationId);
    }
}
