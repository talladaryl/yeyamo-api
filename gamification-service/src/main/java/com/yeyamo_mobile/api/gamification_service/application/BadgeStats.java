package com.yeyamo_mobile.api.gamification_service.application;

public record BadgeStats(
        int earnedBadges,
        int totalBadges,
        long totalXp,
        int level,
        long rank
) {
}
