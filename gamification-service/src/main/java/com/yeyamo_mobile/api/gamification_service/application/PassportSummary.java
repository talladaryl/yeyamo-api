package com.yeyamo_mobile.api.gamification_service.application;

import java.time.Instant;
import java.time.LocalDate;

public record PassportSummary(
        long totalXp,
        int level,
        long currentLevelThreshold,
        long nextLevelThreshold,
        long currentLevelXp,
        long xpToNextLevel,
        int earnedBadgesCount,
        int passportStampsCount,
        int availableRewardsCount,
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate,
        Instant updatedAt) {}
