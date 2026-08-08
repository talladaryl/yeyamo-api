package com.yeyamo_mobile.api.media_service.infrastructure.persistence;

import com.yeyamo_mobile.api.media_service.domain.port.MediaQuotaRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-memory quota repository used in tests and when Redis is not available.
 *
 * <p>Production deployments should replace this with a Redis-backed
 * implementation (INCR + EXPIRE for atomic increments with TTL).
 * This adapter is intentionally simple: no TTL, values reset on restart.</p>
 */
@Component
public class InMemoryMediaQuotaRepository implements MediaQuotaRepository {

    private final ConcurrentHashMap<String, LongAdder> buckets = new ConcurrentHashMap<>();

    @Override
    public long sumBytesForBucket(String bucketKey) {
        LongAdder adder = buckets.get(bucketKey);
        return adder == null ? 0L : adder.longValue();
    }

    @Override
    public void incrementBucket(String bucketKey, long bytes) {
        buckets.computeIfAbsent(bucketKey, k -> new LongAdder()).add(bytes);
    }

    /** Test helper — clears all quota counters. */
    public void reset() {
        buckets.clear();
    }
}
