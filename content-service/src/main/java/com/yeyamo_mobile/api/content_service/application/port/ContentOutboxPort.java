package com.yeyamo_mobile.api.content_service.application.port;
import com.yeyamo_mobile.api.content_service.domain.model.Post;
import java.util.Map;

public interface ContentOutboxPort {
    void append(String eventType, Post post, String correlationId, String actorId);
    
    // Méthode générique pour les événements non-Post (ex: stories)
    void append(String eventType, String aggregateId, String actorId, String correlationId, Map<String, Object> payload);
}
