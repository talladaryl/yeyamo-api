package com.yeyamo_mobile.api.content_service.infrastructure.outbox;

import java.util.Map;
import com.yeyamo_mobile.api.content_service.domain.model.Post;

public interface ContentOutboxPort {
    void append(String eventType, Post post, String correlationId, String actorId);
    void append(String eventType, String aggregateId, String actorId, String correlationId, Map<String, String> payload);
}
