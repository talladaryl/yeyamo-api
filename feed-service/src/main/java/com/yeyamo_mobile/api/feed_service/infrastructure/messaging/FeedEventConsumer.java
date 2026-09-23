package com.yeyamo_mobile.api.feed_service.infrastructure.messaging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.feed_service.application.FeedProjectionCommandService;
import com.yeyamo_mobile.api.feed_service.domain.model.CultureContentLink;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedPost;

@Component
public class FeedEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(FeedEventConsumer.class);
    private static final Set<String> PROJECTABLE_POST_EVENT_TYPES = Set.of(
            "content.post.created",
            "content.post.updated",
            "content.post.published",
            "content.post.visibility_changed",
            "content.post.archived",
            "content.post.deleted");
    private static final Set<String> PROJECTABLE_CULTURE_EVENT_TYPES = Set.of(
            "CultureContentCreated",
            "CultureContentUpdated",
            "CultureContentPublished",
            "CultureContentArchived",
            "CultureContentDeleted");

    private final ObjectMapper mapper;
    private final FeedProjectionCommandService commands;
    private final ProcessedEventRepository processed;

    public FeedEventConsumer(ObjectMapper mapper, FeedProjectionCommandService commands, ProcessedEventRepository processed) {
        this.mapper = mapper;
        this.commands = commands;
        this.processed = processed;
    }

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.content-events:content.events}",
            groupId = "${spring.kafka.consumer.group-id:feed-service}")
    @Transactional
    public void content(String raw) throws Exception {
        JsonNode event = valid(raw, "content-service");
        UUID eventId = UUID.fromString(event.get("eventId").asText());
        if (processed.existsById(eventId)) {
            return;
        }

        String eventType = required(event, "eventType");
        if (!PROJECTABLE_POST_EVENT_TYPES.contains(eventType)) {
            // Stories and other valid content-service events share this topic.
            // They are irrelevant to the post Feed projection and must be
            // acknowledged so that they cannot poison a Kafka partition.
            log.debug("Ignoring unsupported content event for Feed: type={}, eventId={}", eventType, eventId);
            receipt(eventId, eventType);
            return;
        }

        JsonNode payload = event.path("payload");
        UUID postId = UUID.fromString(required(payload, "postId"));
        FeedPost post = new FeedPost(
                postId,
                required(payload, "authorId"),
                text(payload, "caption", null),
                text(payload, "visibility", "PRIVATE"),
                text(payload, "status", "DRAFT"),
                uuid(payload, "catalogAssetId"),
                text(payload, "referenceType", "NONE"),
                text(payload, "referenceId", null),
                uuids(payload, "mediaIds"),
                strings(payload, "hashtags"),
                text(payload, "countryCode", null),
                text(payload, "languageCode", null),
                instant(payload, "publishedAt"),
                instant(event, "occurredAt"));

        commands.content(eventType, post, text(event, "correlationId", eventId.toString()));
        receipt(eventId, eventType);
    }

    @KafkaListener(
            id = "feed-culture-events",
            topics = "${yeyamo.kafka.topics.culture-events:culture.events}",
            groupId = "${spring.kafka.consumer.group-id:feed-service}-culture")
    @Transactional
    public void culture(String raw) throws Exception {
        JsonNode event = valid(raw, "culture-service");
        UUID eventId = UUID.fromString(event.get("eventId").asText());
        if (processed.existsById(eventId)) {
            return;
        }
        String eventType = required(event, "eventType");
        if (!PROJECTABLE_CULTURE_EVENT_TYPES.contains(eventType)) {
            // culture.events also carries translations, challenges and language
            // learning events. They are valid, but do not belong in this Feed
            // projection and must advance the offset without schema parsing.
            log.debug("Ignoring unsupported culture event for Feed: type={}, eventId={}", eventType, eventId);
            receipt(eventId, eventType);
            return;
        }
        JsonNode payload = event.path("payload");
        String type = required(payload, "type");
        if ("PROVERB".equals(type) || "RECIPE".equals(type)) {
            UUID contentId = UUID.fromString(required(payload, "contentId"));
            commands.culture(
                    new CultureContentLink(contentId, type, text(payload, "title", null), payload.path("isActive").asBoolean(false)),
                    text(event, "correlationId", eventId.toString()));
        }
        receipt(eventId, eventType);
    }

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.interaction-events:interaction.events}",
            groupId = "${spring.kafka.consumer.group-id:feed-service}")
    @Transactional
    public void interaction(String raw) throws Exception {
        JsonNode event = valid(raw, "interaction-service");
        UUID eventId = UUID.fromString(event.get("eventId").asText());
        if (processed.existsById(eventId)) {
            return;
        }
        String eventType = required(event, "eventType");
        JsonNode payload = event.path("payload");
        UUID postId = uuid(payload, "postId");
        if (postId == null) {
            receipt(eventId, eventType);
            return;
        }
        String user = text(payload, "userId", text(payload, "authorId", text(event, "actorId", null)));
        commands.interaction(eventType, postId, user, text(event, "correlationId", eventId.toString()));
        receipt(eventId, eventType);
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.user-events:user.events}",
            groupId = "${spring.kafka.consumer.group-id:feed-service}-social")
    @Transactional
    public void user(String raw) throws Exception {
        JsonNode event = valid(raw, "user-service");
        UUID eventId = UUID.fromString(event.get("eventId").asText());
        if (processed.existsById(eventId)) return;
        String eventType = required(event, "eventType");
        JsonNode payload = event.path("payload");
        if ("social.muted".equals(eventType) || "social.unmuted".equals(eventType)) {
            commands.mute(required(payload, "muterAuthUserId"), required(payload, "mutedAuthUserId"),
                    "social.muted".equals(eventType));
        }
        receipt(eventId, eventType);
    }

    private JsonNode valid(String raw, String producer) throws Exception {
        JsonNode event = mapper.readTree(raw);
        required(event, "eventId");
        if (event.path("eventVersion").asInt(0) != 1) {
            throw new IllegalArgumentException("Unsupported event version");
        }
        if (!producer.equals(required(event, "producer"))) {
            throw new IllegalArgumentException("Unexpected producer");
        }
        return event;
    }

    private void receipt(UUID eventId, String eventType) {
        ProcessedEventEntity receipt = new ProcessedEventEntity();
        receipt.setEventId(eventId);
        receipt.setEventType(eventType);
        receipt.setProcessedAt(Instant.now());
        processed.save(receipt);
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? defaultValue : value.asText();
    }

    private UUID uuid(JsonNode node, String field) {
        String value = text(node, field, null);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private Instant instant(JsonNode node, String field) {
        String value = text(node, field, null);
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private List<UUID> uuids(JsonNode node, String field) {
        List<UUID> result = new ArrayList<>();
        JsonNode values = node.path(field);
        if (values.isArray()) {
            values.forEach(value -> result.add(UUID.fromString(value.asText())));
        }
        return result;
    }

    private List<String> strings(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        JsonNode values = node.path(field);
        if (values.isArray()) {
            values.forEach(value -> result.add(value.asText()));
        }
        return result;
    }
}
