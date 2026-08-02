package com.yeyamo_mobile.api.gamification_service.application;

public record LeaderboardEntry(long rank, String userId, long totalXp, int level) {
}
