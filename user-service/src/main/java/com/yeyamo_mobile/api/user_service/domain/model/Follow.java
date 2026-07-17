package com.yeyamo_mobile.api.user_service.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Follow(
    UUID followerId,
    UUID followeeId,
    Instant createdAt
) {
    public static Follow create(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        return new Follow(followerId, followeeId, Instant.now());
    }
}
