package com.yeyamo_mobile.api.mission_reward_service.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MissionEvent(UUID eventId, String eventType, String userId,
        Map<String, Object> payload, Instant occurredAt, String correlationId) {}
