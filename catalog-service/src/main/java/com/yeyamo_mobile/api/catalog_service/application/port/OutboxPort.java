package com.yeyamo_mobile.api.catalog_service.application.port;

import java.util.Map;

public interface OutboxPort {
    void append(String eventType, String aggregateId, String actorId, String correlationId, Map<String, String> payload);
}
