package com.yeyamo_mobile.api.media_service.domain.port;

/**
 * Port for quota accounting storage.
 * The implementation may delegate to Redis (atomic increments, TTL-based expiry)
 * or a SQL table — the application layer does not care.
 */
public interface MediaQuotaRepository {

    /** Returns total bytes already stored for a given quota bucket key. */
    long sumBytesForBucket(String bucketKey);

    /**
     * Atomically adds {@code bytes} to the bucket.
     * The implementation is responsible for setting appropriate TTL
     * (e.g. 25 hours for daily buckets, 35 days for monthly buckets).
     */
    void incrementBucket(String bucketKey, long bytes);
}
