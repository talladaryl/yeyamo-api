package com.yeyamo_mobile.api.gamification_service.application;

import java.time.Instant;

public record BadgeCatalogEntry(
        String code,
        String name,
        String description,
        boolean earned,
        Instant earnedAt
) {
}
