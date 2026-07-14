package com.yeyamo_mobile.api.user_service.application.port;

import java.util.Map;
import java.util.UUID;

public interface OutboxPort {
    void append(String eventType, UUID profileId, String actorId, String correlationId, Map<String, Object> payload);
}
