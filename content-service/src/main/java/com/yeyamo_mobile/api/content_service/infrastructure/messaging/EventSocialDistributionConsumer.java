package com.yeyamo_mobile.api.content_service.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.content_service.application.EventPublishedSocialCommand;
import com.yeyamo_mobile.api.content_service.application.EventSocialDistributionService;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.ContentProcessedEventReceiptRepository;

/** Consumes only the explicit publication command emitted by event-service. */
@Component
public class EventSocialDistributionConsumer {
    private static final String EVENT_PUBLISHED = "event.published";

    private final ObjectMapper mapper;
    private final EventSocialDistributionService distribution;
    private final ContentProcessedEventReceiptRepository receipts;

    public EventSocialDistributionConsumer(
            ObjectMapper mapper,
            EventSocialDistributionService distribution,
            ContentProcessedEventReceiptRepository receipts) {
        this.mapper = mapper;
        this.distribution = distribution;
        this.receipts = receipts;
    }

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.event-events:event.events}",
            groupId = "${spring.kafka.consumer.group-id:content-service}-event-social")
    public void consume(String raw) throws Exception {
        JsonNode event = validEnvelope(raw);
        String eventType = required(event, "eventType");
        if (!EVENT_PUBLISHED.equals(eventType)) {
            return;
        }

        UUID sourceEventId = UUID.fromString(required(event, "eventId"));
        if (receipts.existsById(sourceEventId)) {
            return;
        }

        JsonNode payload = event.path("payload");
        if (!"PUBLISHED".equals(required(payload, "status"))) {
            throw new IllegalArgumentException("event.published payload status must be PUBLISHED");
        }
        EventPublishedSocialCommand command = new EventPublishedSocialCommand(
                sourceEventId,
                UUID.fromString(required(payload, "eventId")),
                required(payload, "organizerUserId"),
                required(payload, "title"),
                text(payload, "description"),
                instant(payload, "startAt"),
                instant(payload, "endAt"),
                "PUBLIC".equals(required(payload, "visibility")),
                uuid(payload, "coverMediaId"),
                payload.path("publishToFeed").asBoolean(false),
                payload.path("publishToStory").asBoolean(false),
                text(payload, "countryCode"),
                text(payload, "languageCode"),
                text(event, "correlationId") == null ? sourceEventId.toString() : text(event, "correlationId"));
        distribution.distribute(command);
    }

    private JsonNode validEnvelope(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        if (event.path("eventVersion").asInt(0) != 1) {
            throw new IllegalArgumentException("Unsupported event version");
        }
        if (!"event-service".equals(required(event, "producer"))) {
            throw new IllegalArgumentException("Unexpected producer");
        }
        required(event, "eventId");
        required(event, "eventType");
        required(event, "occurredAt");
        return event;
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private Instant instant(JsonNode node, String field) {
        String value = required(node, field);
        return Instant.parse(value);
    }
}
