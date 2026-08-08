package com.yeyamo_mobile.api.notification_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.notification_service.application.NotificationApplicationService;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationEventConsumer {
    private final ObjectMapper mapper;
    private final EventNotificationPolicy policy;
    private final NotificationApplicationService service;
    private final ProcessedEventRepository processed;

    public NotificationEventConsumer(ObjectMapper mapper, EventNotificationPolicy policy,
            NotificationApplicationService service, ProcessedEventRepository processed) {
        this.mapper = mapper;
        this.policy = policy;
        this.service = service;
        this.processed = processed;
    }

    @KafkaListener(topics = {
            "${yeyamo.kafka.topics.auth-events:auth.events}",
            "${yeyamo.kafka.topics.partner-events:partner-events}",
            "${yeyamo.kafka.topics.moderation-events:moderation.events}",
            "${yeyamo.kafka.topics.messaging-events:messaging.events}",
            "${yeyamo.kafka.topics.culture-events:culture.events}",
            "${yeyamo.kafka.topics.catalog-events:catalog.events}",
            "${yeyamo.kafka.topics.commerce-events:commerce.events}",
            "${yeyamo.kafka.topics.interaction-events:interaction.events}",
            "${yeyamo.kafka.topics.artisan-events:artisan.events}" },
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        UUID id = UUID.fromString(required(event, "eventId"));
        if (processed.existsById(id)) {
            return;
        }
        String type = required(event, "eventType");
        String correlationId = event.path("correlationId").asText(id.toString());
        MDC.put("correlationId", correlationId);
        try {
            for (var intent : policy.map(event)) {
                service.create(intent);
            }
            processed.save(new ProcessedEventEntity(id, type));
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.asText();
    }
}
