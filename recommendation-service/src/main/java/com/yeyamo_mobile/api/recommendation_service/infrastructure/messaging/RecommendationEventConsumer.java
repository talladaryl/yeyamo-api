package com.yeyamo_mobile.api.recommendation_service.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePlanAvailabilityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.recommendation_service.application.RecommendationProjectionService;
import com.yeyamo_mobile.api.recommendation_service.domain.Candidate;
import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;

/** Materializes only the compact candidate data needed by Recommendation and Adventure Plan ranking. */
@Component
public class RecommendationEventConsumer {
    private final ObjectMapper mapper;
    private final RecommendationProjectionService service;
    private final ProcessedEventRepository processed;
    private final AdventurePlanAvailabilityService adventureAvailability;

    public RecommendationEventConsumer(ObjectMapper mapper, RecommendationProjectionService service,
            ProcessedEventRepository processed) {
        this(mapper, service, processed, null);
    }

    @Autowired
    public RecommendationEventConsumer(ObjectMapper mapper, RecommendationProjectionService service,
            ProcessedEventRepository processed, AdventurePlanAvailabilityService adventureAvailability) {
        this.mapper = mapper;
        this.service = service;
        this.processed = processed;
        this.adventureAvailability = adventureAvailability;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.catalog-events:catalog.events}", groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void catalog(String raw) throws Exception { handle(raw, "catalog"); }

    @KafkaListener(topics = "${yeyamo.kafka.topics.content-events:content.events}", groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void content(String raw) throws Exception { handle(raw, "content"); }

    @KafkaListener(topics = "${yeyamo.kafka.topics.interaction-events:interaction.events}", groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void interaction(String raw) throws Exception { handle(raw, "interaction"); }

    @KafkaListener(topics = "${yeyamo.kafka.topics.feed-events:feed.events}", groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void feed(String raw) throws Exception { handle(raw, "feed"); }

    @KafkaListener(topics = "${yeyamo.kafka.topics.user-events:user.events}", groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void user(String raw) throws Exception { handle(raw, "user"); }

    /** Event projections are deliberately compact: no Event table is duplicated in this service. */
    @KafkaListener(topics = "${yeyamo.kafka.topics.event-events:event.events}", groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void event(String raw) throws Exception { handle(raw, "event"); }

    private void handle(String raw, String source) throws Exception {
        JsonNode envelope = mapper.readTree(raw);
        UUID eventId = UUID.fromString(required(envelope, "eventId"));
        if (processed.existsById(eventId)) return;
        if (envelope.path("eventVersion").asInt(0) != 1) throw new IllegalArgumentException("Unsupported event version");

        String eventType = required(envelope, "eventType");
        JsonNode payload = envelope.path("payload");
        switch (source) {
            case "catalog" -> catalog(eventType, payload);
            case "content" -> content(eventType, payload);
            case "interaction" -> interaction(eventType, payload);
            case "feed" -> feed(eventType, payload);
            case "user" -> user(eventType, payload);
            case "event" -> event(eventType, payload, required(envelope, "producer"));
            default -> throw new IllegalArgumentException("Unsupported source");
        }
        processed.save(new ProcessedEventEntity(eventId, eventType));
    }

    private void catalog(String eventType, JsonNode payload) {
        if (!eventType.startsWith("catalog.asset.")) return;
        String assetId = required(payload, "assetId");
        boolean active = !eventType.endsWith("deleted") && "PUBLISHED".equals(text(payload, "status", "DRAFT"));
        service.candidate(new Candidate(
                "catalog:" + assetId, assetId, kind(text(payload, "type", "PLACE")),
                required(payload, "name"), text(payload, "categoryCode", null), text(payload, "regionCode", null),
                text(payload, "countryCode", null), text(payload, "languageCode", null), number(payload, "latitude"), number(payload, "longitude"),
                0, active, active ? Instant.now() : null, Instant.now(), decimal(payload, "price"),
                text(payload, "currency", null), firstUuid(payload, "mediaIds"), location(payload), null, null));
    }

    private void content(String eventType, JsonNode payload) {
        if (!eventType.startsWith("content.post.")) return;
        String postId = required(payload, "postId");
        boolean active = !eventType.endsWith("deleted")
                && "PUBLISHED".equals(text(payload, "status", "DRAFT"))
                && "PUBLIC".equals(text(payload, "visibility", "PRIVATE"));
        service.candidate(new Candidate("content:" + postId, postId, CandidateKind.CONTENT,
                text(payload, "caption", "Publication"), null, null, text(payload, "countryCode", null),
                text(payload, "languageCode", null), null, null, 0, active, instant(payload, "publishedAt"), Instant.now()));
    }

    private void event(String eventType, JsonNode payload, String producer) {
        if (!"event-service".equals(producer)) throw new IllegalArgumentException("Unexpected event producer");
        if (!java.util.Set.of("event.created", "event.updated", "event.published", "event.cancelled", "event.completed")
                .contains(eventType)) return;
        String eventId = required(payload, "eventId");
        boolean active = "PUBLISHED".equals(text(payload, "status", null))
                && "PUBLIC".equals(text(payload, "visibility", null));
        service.candidate(new Candidate("event:" + eventId, eventId, CandidateKind.EVENT,
                required(payload, "title"), null, null, text(payload, "countryCode", null), text(payload, "languageCode", null),
                number(payload, "latitude"), number(payload, "longitude"), 0, active,
                instant(payload, "startAt"), Instant.now(), null, null, uuid(payload, "coverMediaId"),
                text(payload, "locationName", null), instant(payload, "startAt"), instant(payload, "endAt")));
        if (adventureAvailability != null && ("event.cancelled".equals(eventType)
                || "CANCELLED".equals(text(payload, "status", null)))) {
            adventureAvailability.markEventUnavailable(eventId);
        } else if (adventureAvailability != null && "event.completed".equals(eventType)) {
            adventureAvailability.markEventCompleted(eventId);
        }
    }

    private void interaction(String eventType, JsonNode payload) {
        if ("interaction.feedback.updated".equals(eventType)) {
            service.feedback(required(payload, "userId"), required(payload, "targetType"), required(payload, "targetId"),
                    required(payload, "feedbackType"), text(payload, "previousFeedbackType", ""));
            return;
        }
        if ("interaction.feedback.removed".equals(eventType)) {
            service.removeFeedback(required(payload, "userId"), required(payload, "targetType"), required(payload, "targetId"),
                    text(payload, "previousFeedbackType", ""));
            return;
        }
        String source;
        String user = text(payload, "userId", text(payload, "authorId", null));
        double delta = switch (eventType) {
            case "interaction.like.added" -> 2;
            case "interaction.like.removed" -> -2;
            case "interaction.favorite.added" -> 3;
            case "interaction.favorite.removed" -> -3;
            case "interaction.comment.created" -> 1;
            case "interaction.comment.deleted" -> -1;
            case "interaction.post.shared" -> 4;
            case "interaction.checkin.created" -> 5;
            default -> 0;
        };
        if (delta == 0) return;
        source = eventType.equals("interaction.checkin.created") ? "catalog:" + required(payload, "catalogAssetId")
                : "content:" + required(payload, "postId");
        service.popularity(source, delta);
        service.signal(user, source, delta);
    }

    private void feed(String eventType, JsonNode payload) {
        if ("feed.metrics.updated".equals(eventType)) service.popularity("content:" + required(payload, "postId"), 0.25);
    }

    private void user(String eventType, JsonNode payload) {
        if (!eventType.startsWith("profile.") && !eventType.startsWith("User")) return;
        String user = required(payload, "authUserId");
        if (eventType.equals("profile.location_updated") || eventType.equals("UserLocationUpdated") || eventType.equals("UserCountrySelected")) {
            service.countryPreferences(user, text(payload, "countryCode", null), null, null);
        } else if (eventType.equals("profile.language_updated") || eventType.equals("UserLanguageUpdated")) {
            service.countryPreferences(user, null, null, strings(payload, "contentLanguages"));
        } else if (eventType.equals("profile.discovery_preferences_updated") || eventType.equals("UserDiscoveryPreferencesUpdated")) {
            service.countryPreferences(user, text(payload, "countryCode", null), strings(payload, "contentCountries"), null);
        } else {
            service.preference(user, text(payload, "preferredRegionId", null),
                    text(payload, "language", text(payload, "preferredLanguageCode", null)),
                    payload.path("locationSharingEnabled").asBoolean(false));
        }
    }

    private CandidateKind kind(String value) {
        try { return CandidateKind.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { return CandidateKind.PLACE; }
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field, null);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
    }

    private Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isNumber() ? null : value.asDouble();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        try { return value.decimalValue(); } catch (RuntimeException ignored) { return null; }
    }

    private Instant instant(JsonNode node, String field) {
        try {
            String value = text(node, field, null);
            return value == null ? null : Instant.parse(value);
        } catch (RuntimeException ignored) { return null; }
    }

    private UUID uuid(JsonNode node, String field) {
        try {
            String value = text(node, field, null);
            return value == null ? null : UUID.fromString(value);
        } catch (RuntimeException ignored) { return null; }
    }

    private UUID firstUuid(JsonNode node, String field) {
        JsonNode values = node.path(field);
        if (!values.isArray() || values.isEmpty()) return null;
        try { return UUID.fromString(values.get(0).asText()); } catch (RuntimeException ignored) { return null; }
    }

    private String location(JsonNode node) {
        String city = text(node, "city", null);
        return city != null ? city : text(node, "address", null);
    }

    private Set<String> strings(JsonNode node, String field) {
        Set<String> values = new LinkedHashSet<>();
        JsonNode list = node.path(field);
        if (list.isArray()) list.forEach(value -> {
            if (!value.isNull() && !value.asText().isBlank()) values.add(value.asText());
        });
        return values;
    }
}
