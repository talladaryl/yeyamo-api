package com.yeyamo_mobile.api.event_service.event;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.event_service.enums.SocialDistributionStatus;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.models.EventSocialDistributionReceipt;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;
import com.yeyamo_mobile.api.event_service.repository.EventSocialDistributionReceiptRepository;

/**
 * Applies only result events emitted by content-service. It never consumes its
 * own `event.events`, preventing a command/result loop.
 */
@Component
public class ContentSocialDistributionResultConsumer {
    private static final Logger log = LoggerFactory.getLogger(ContentSocialDistributionResultConsumer.class);
    private static final String RESULT_EVENT = "content.event_social.distribution.updated";

    private final ObjectMapper mapper;
    private final EventRepository events;
    private final EventSocialDistributionReceiptRepository receipts;

    public ContentSocialDistributionResultConsumer(
            ObjectMapper mapper,
            EventRepository events,
            EventSocialDistributionReceiptRepository receipts) {
        this.mapper = mapper;
        this.events = events;
        this.receipts = receipts;
    }

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.content-events:content.events}",
            groupId = "${spring.kafka.consumer.group-id:event-service}-social-results")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = validEnvelope(raw);
        if (!RESULT_EVENT.equals(required(event, "eventType"))) {
            return;
        }

        UUID resultEventId = UUID.fromString(required(event, "eventId"));
        if (receipts.existsById(resultEventId)) {
            return;
        }

        JsonNode payload = event.path("payload");
        UUID eventId = UUID.fromString(required(payload, "eventId"));
        String target = required(payload, "target");
        SocialDistributionStatus status = SocialDistributionStatus.valueOf(required(payload, "status"));
        UUID contentId = uuid(payload, "contentId");
        String reason = text(payload, "reason");

        events.findByIdForUpdate(eventId).ifPresentOrElse(
                domainEvent -> apply(domainEvent, target, status, contentId, reason),
                () -> log.info("Ignoring social distribution result for missing eventId={}, resultEventId={}", eventId, resultEventId));
        receipt(resultEventId, RESULT_EVENT);
    }

    private void apply(Event event, String target, SocialDistributionStatus status, UUID contentId, String reason) {
        if ("FEED".equals(target)) {
            if (!event.isPublishToFeed() || terminal(event.getFeedDistributionStatus())) return;
            event.setFeedDistributionStatus(status);
            event.setFeedDistributionReason(reason);
            if (status == SocialDistributionStatus.PUBLISHED) event.setFeedPostId(contentId);
            events.save(event);
            return;
        }
        if ("STORY".equals(target)) {
            if (!event.isPublishToStory() || terminal(event.getStoryDistributionStatus())) return;
            event.setStoryDistributionStatus(status);
            event.setStoryDistributionReason(reason);
            if (status == SocialDistributionStatus.PUBLISHED) event.setStoryId(contentId);
            events.save(event);
            return;
        }
        throw new IllegalArgumentException("Unsupported social distribution target");
    }

    private boolean terminal(SocialDistributionStatus status) {
        return status == SocialDistributionStatus.PUBLISHED
                || status == SocialDistributionStatus.SKIPPED_NO_MEDIA
                || status == SocialDistributionStatus.SKIPPED_PRIVATE_EVENT;
    }

    private JsonNode validEnvelope(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        if (event.path("eventVersion").asInt(0) != 1) {
            throw new IllegalArgumentException("Unsupported event version");
        }
        if (!"content-service".equals(required(event, "producer"))) {
            throw new IllegalArgumentException("Unexpected producer");
        }
        required(event, "eventId");
        required(event, "occurredAt");
        return event;
    }

    private void receipt(UUID eventId, String eventType) {
        EventSocialDistributionReceipt receipt = new EventSocialDistributionReceipt();
        receipt.setEventId(eventId);
        receipt.setEventType(eventType);
        receipt.setProcessedAt(Instant.now());
        receipts.save(receipt);
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
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
}
