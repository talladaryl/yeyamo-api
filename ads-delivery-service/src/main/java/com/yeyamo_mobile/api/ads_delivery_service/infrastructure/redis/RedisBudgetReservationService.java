package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.redis;

import com.yeyamo_mobile.api.ads_delivery_service.domain.port.BudgetReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.redis.core.script.DefaultRedisScript;

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
    public boolean reserveBudget(String campaignId, BigDecimal amount,
            BigDecimal maximumBudget, String reservationId) {
        String key = buildReservationKey(reservationId);
        String spentKey = "budget:spent:" + campaignId;
        long amountUnits = units(amount);
        long maximumUnits = units(maximumBudget);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then return 1 end
            local spent = tonumber(redis.call('GET', KEYS[2]) or '0')
            local amount = tonumber(ARGV[1])
            local maximum = tonumber(ARGV[2])
            if amount < 0 or spent + amount > maximum then return 0 end
            redis.call('INCRBY', KEYS[2], amount)
            redis.call('SET', KEYS[1], KEYS[2] .. ':' .. ARGV[1],
                       'EX', ARGV[3])
            return 1
            """, Long.class);
        Long result = redisTemplate.execute(script, List.of(key, spentKey),
            Long.toString(amountUnits), Long.toString(maximumUnits),
            Long.toString(reservationTtlSeconds));
        boolean success = Long.valueOf(1).equals(result);
        if (success) {
            logger.debug("Reserved budget {} for campaign {} with reservation {}", amount, campaignId, reservationId);
        } else {
            logger.warn("Budget exhausted or reservation rejected: {}", reservationId);
        }
        return success;
    }

    @Override
    public void confirmReservation(String reservationId, BigDecimal actualAmount) {
        String key = buildReservationKey(reservationId);
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            redisTemplate.delete(key);
            logger.debug("Confirmed reservation {} with actual amount {}",
                reservationId, actualAmount);
        }
    }

    @Override
    public void releaseReservation(String reservationId) {
        String key = buildReservationKey(reservationId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if not value then return 0 end
            local separator = string.find(value, ':', 1, true)
            local spentKey = string.sub(value, 1, separator - 1)
            local amount = tonumber(string.sub(value, separator + 1))
            redis.call('DECRBY', spentKey, amount)
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);
        redisTemplate.execute(script, List.of(key));
        
        logger.debug("Released reservation {}", reservationId);
    }

    private String buildReservationKey(String reservationId) {
        return String.format("budget:reservation:%s", reservationId);
    }

    private long units(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Budget amount must be non-negative");
        }
        return value.setScale(4, RoundingMode.HALF_UP)
            .movePointRight(4).longValueExact();
    }
}
