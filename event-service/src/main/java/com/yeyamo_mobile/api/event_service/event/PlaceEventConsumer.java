package com.yeyamo_mobile.api.event_service.event;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.event_service.models.PlaceReadModel;
import com.yeyamo_mobile.api.event_service.repository.PlaceReadModelRepository;

@Component
public class PlaceEventConsumer {
    private final ObjectMapper mapper;
    private final PlaceReadModelRepository places;

    public PlaceEventConsumer(ObjectMapper mapper, PlaceReadModelRepository places) {
        this.mapper = mapper;
        this.places = places;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.place-events:place.events}",
            groupId = "${spring.kafka.consumer.group-id:event-service}")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        if (!event.path("eventType").asText("").startsWith("place.")) {
            return;
        }
        JsonNode payload = event.path("payload");
        String rawPlaceId = text(payload, "placeId");
        if (rawPlaceId == null) rawPlaceId = text(event, "aggregateId");
        if (rawPlaceId == null) {
            throw new IllegalArgumentException("placeId is required in place event");
        }
        UUID placeId = UUID.fromString(rawPlaceId);
        String eventType = event.path("eventType").asText();
        boolean active = "PUBLISHED".equalsIgnoreCase(text(payload, "status"))
                && !"place.deleted".equalsIgnoreCase(eventType);
        Instant updatedAt = instant(text(payload, "updatedAt"));
        PlaceReadModel place = places.findById(placeId)
                .orElseGet(() -> new PlaceReadModel(placeId, "Place " + placeId, active, updatedAt));
        String name = text(payload, "name");
        if (name != null) place.setName(name);
        place.setActive(active);
        place.setUpdatedAt(updatedAt);
        places.save(place);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Instant instant(String value) {
        try { return value == null || value.isBlank() ? Instant.now() : Instant.parse(value); }
        catch (RuntimeException exception) { return Instant.now(); }
    }
}
