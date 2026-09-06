package com.yeyamo_mobile.api.booking_service.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.PlaceReadModelEntity;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.PlaceReadModelRepository;

@Component
public class PlaceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlaceEventConsumer.class);

    private final ObjectMapper mapper;
    private final PlaceReadModelRepository placeReadModelRepository;
    private final ProcessedEventRepository processed;

    public PlaceEventConsumer(
            ObjectMapper mapper,
            PlaceReadModelRepository placeReadModelRepository,
            ProcessedEventRepository processed
    ) {
        this.mapper = mapper;
        this.placeReadModelRepository = placeReadModelRepository;
        this.processed = processed;
    }

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.place-events:place.events}",
            groupId = "${spring.kafka.consumer.group-id:booking-service}"
    )
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode root = mapper.readTree(raw);
        String eventIdStr = text(root, "eventId");
        if (eventIdStr == null || eventIdStr.isBlank()) {
            log.warn("Ignoring place event without eventId: {}", raw);
            return;
        }

        UUID eventId = UUID.fromString(eventIdStr);
        if (processed.existsById(eventId)) {
            log.debug("Place event {} already processed, skipping", eventId);
            return;
        }

        String eventType = text(root, "eventType");
        if (eventType == null || !eventType.startsWith("place.")) {
            log.debug("Ignoring non-place event type: {}", eventType);
            return;
        }

        JsonNode payload = root.path("payload");
        String placeIdStr = text(payload, "placeId");
        if (placeIdStr == null || placeIdStr.isBlank()) {
            placeIdStr = text(root, "aggregateId");
        }
        if (placeIdStr == null || placeIdStr.isBlank()) {
            log.warn("Ignoring place event without placeId in payload or aggregateId: {}", raw);
            return;
        }

        UUID placeId = UUID.fromString(placeIdStr);
        String name = text(payload, "name");
        String status = text(payload, "status");
        Instant updatedAt = parseInstant(text(payload, "updatedAt"));

        boolean active = "PUBLISHED".equalsIgnoreCase(status) && !"place.deleted".equalsIgnoreCase(eventType);

        upsertPlace(placeId, name != null ? name : "Place " + placeId, active, updatedAt);

        processed.save(new ProcessedEventEntity(eventId, eventType));
    }

    @Transactional
    public void upsertPlace(UUID placeId, String name, boolean active, Instant updatedAt) {
        Instant timestamp = updatedAt != null ? updatedAt : Instant.now();
        PlaceReadModelEntity entity = placeReadModelRepository.findById(placeId)
                .orElseGet(() -> new PlaceReadModelEntity(placeId, name, active, timestamp));

        entity.setName(name != null ? name : entity.getName());
        entity.setActive(active);
        entity.setUpdatedAt(timestamp);

        placeReadModelRepository.save(entity);
        log.info("Synchronized place_read_model for placeId={}, name={}, active={}", placeId, entity.getName(), active);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Instant parseInstant(String text) {
        if (text == null || text.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(text);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
