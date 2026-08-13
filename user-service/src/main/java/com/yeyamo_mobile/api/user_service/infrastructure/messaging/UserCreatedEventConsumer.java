package com.yeyamo_mobile.api.user_service.infrastructure.messaging;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.user_service.application.UserProfileService;

@Component
public class UserCreatedEventConsumer {
    private final ObjectMapper objectMapper;
    private final UserProfileService profileService;
    private final ProcessedEventRepository processedEvents;

    public UserCreatedEventConsumer(ObjectMapper objectMapper, UserProfileService profileService,
            ProcessedEventRepository processedEvents) {
        this.objectMapper = objectMapper;
        this.profileService = profileService;
        this.processedEvents = processedEvents;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.user-events:user-events}",
            groupId = "${spring.kafka.consumer.group-id:user-service}")
    @Transactional
    public void consume(String rawEvent) throws Exception {
        JsonNode event = objectMapper.readTree(rawEvent);
        if (!"user.created".equals(text(event, "eventType"))) return;
        UUID eventId = UUID.fromString(required(event, "eventId"));
        if (processedEvents.existsById(eventId)) return;

        JsonNode payload = event.path("payload");
        String authUserId = first(payload, "userId", "id", "authUserId");
        String displayName = firstOptional(payload, "displayName", "name", "email");
        
        // Get geographic data from auth-service event
        String countryCode = text(payload, "countryCode");
        String cityIdStr = text(payload, "cityId");
        String preferredLanguageCode = text(payload, "preferredLanguageCode");
        String timezone = text(payload, "timezone");
        
        UUID cityId = null;
        if (cityIdStr != null && !cityIdStr.isBlank()) {
            try {
                cityId = UUID.fromString(cityIdStr);
            } catch (IllegalArgumentException ignored) {
                // Log and continue without cityId
            }
        }
        
        // Create profile with geographic data
        profileService.createFromIdentityWithLocation(authUserId, displayName, countryCode, 
                cityId, preferredLanguageCode, timezone, text(event, "correlationId"));
        processedEvents.save(new ProcessedEventEntity(eventId, "user.created"));
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
    private String first(JsonNode node, String... fields) {
        String value = firstOptional(node, fields);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("User identity is required");
        return value;
    }
    private String firstOptional(JsonNode node, String... fields) {
        for (String field : fields) { String value = text(node, field); if (value != null && !value.isBlank()) return value; }
        return null;
    }
    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
