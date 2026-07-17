package com.yeyamo_mobile.api.user_service.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Block(
    UUID blockerId,
    UUID blockedId,
    Instant createdAt
) {
    public static Block create(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
        return new Block(blockerId, blockedId, Instant.now());
    }
}
