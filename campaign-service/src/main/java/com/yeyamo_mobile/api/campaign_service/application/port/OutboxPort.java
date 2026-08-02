package com.yeyamo_mobile.api.campaign_service.application.port;

import java.util.Map;
import java.util.UUID;

/**
 * Port for Transactional Outbox Pattern
 */
public interface OutboxPort {
    void append(String eventType, UUID aggregateId, String actorId, String correlationId, Map<String, Object> payload);
}
