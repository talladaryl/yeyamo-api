package com.yeyamo_mobile.api.gamification_service.domain;

import java.time.Instant;
import java.util.UUID;

public record XpHistoryEntry(UUID id, int points, String reason, String sourceId, Instant occurredAt) {}
