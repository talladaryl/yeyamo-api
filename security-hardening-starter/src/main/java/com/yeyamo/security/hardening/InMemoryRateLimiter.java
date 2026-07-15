package com.yeyamo.security.hardening;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryRateLimiter {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Bucket overflow = new Bucket(1, 1);
    private final int maxTrackedClients;

    InMemoryRateLimiter(int maxTrackedClients) {
        this.maxTrackedClients = Math.max(1_000, maxTrackedClients);
    }

    boolean tryAcquire(String key, int permitsPerMinute, int burst) {
        int safePermits = Math.max(1, permitsPerMinute);
        int safeBurst = Math.max(1, burst);
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= maxTrackedClients) {
                return overflow.tryAcquire(safePermits, safeBurst);
            }
            bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(safePermits, safeBurst));
        }
        return bucket.tryAcquire(safePermits, safeBurst);
    }

    private static final class Bucket {
        private double tokens;
        private long updatedAtNanos;

        Bucket(int permitsPerMinute, int burst) {
            tokens = Math.max(1, burst);
            updatedAtNanos = System.nanoTime();
        }

        synchronized boolean tryAcquire(int permitsPerMinute, int burst) {
            long now = System.nanoTime();
            double refillPerNano = permitsPerMinute / (double) Duration.ofMinutes(1).toNanos();
            tokens = Math.min(burst, tokens + ((now - updatedAtNanos) * refillPerNano));
            updatedAtNanos = now;
            if (tokens < 1) {
                return false;
            }
            tokens -= 1;
            return true;
        }
    }
}
