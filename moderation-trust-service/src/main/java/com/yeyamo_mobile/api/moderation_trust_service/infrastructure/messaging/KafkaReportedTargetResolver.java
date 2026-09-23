package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.moderation_trust_service.application.ModerationException;
import com.yeyamo_mobile.api.moderation_trust_service.application.ReportedTarget;
import com.yeyamo_mobile.api.moderation_trust_service.application.ReportedTargetResolver;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence.ReportedTargetEntity;
import com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence.ReportedTargetRepository;

/** Idempotent local read model. It never accepts an owner supplied by a mobile client. */
@Component
public class KafkaReportedTargetResolver implements ReportedTargetResolver {
    private final ObjectMapper mapper; private final ReportedTargetRepository targets; private final ProcessedEventRepository processed;
    public KafkaReportedTargetResolver(ObjectMapper mapper, ReportedTargetRepository targets, ProcessedEventRepository processed) { this.mapper = mapper; this.targets = targets; this.processed = processed; }

    @Override public ReportedTarget require(TargetType type, String targetId) {
        if (type == TargetType.USER) return new ReportedTarget(targetId);
        ReportedTargetEntity entity = targets.findById(key(type, targetId)).filter(ReportedTargetEntity::isAvailable)
                .orElseThrow(() -> new ModerationException("REPORT_TARGET_NOT_FOUND", "The target is unavailable or has not been projected"));
        return new ReportedTarget(entity.getOwnerId());
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.content-events:content.events}", groupId = "${spring.kafka.consumer.group-id:moderation-trust-service}-report-targets-content")
    @Transactional public void content(String raw) throws Exception { handle(raw, "content-service"); }
    @KafkaListener(topics = "${yeyamo.kafka.topics.event-events:event.events}", groupId = "${spring.kafka.consumer.group-id:moderation-trust-service}-report-targets-event")
    @Transactional public void event(String raw) throws Exception { handle(raw, "event-service"); }
    @KafkaListener(topics = "${yeyamo.kafka.topics.place-events:place.events}", groupId = "${spring.kafka.consumer.group-id:moderation-trust-service}-report-targets-place")
    @Transactional public void place(String raw) throws Exception { handle(raw, "place-service"); }

    private void handle(String raw, String producer) throws Exception {
        JsonNode event = mapper.readTree(raw); if (!producer.equals(text(event, "producer"))) throw new IllegalArgumentException("Unexpected producer");
        // place-service currently publishes its enriched place envelope as v2;
        // v1 payload fields used here are preserved. Reject only invalid/missing versions.
        if (event.path("eventVersion").asInt() < 1) throw new IllegalArgumentException("Unsupported event version");
        UUID eventId = UUID.fromString(required(event, "eventId")); if (processed.existsById(eventId)) return;
        String eventType = required(event, "eventType"); JsonNode payload = event.path("payload");
        if ("content.post.created".equals(eventType) || eventType.startsWith("content.post.")) upsert(TargetType.POST, required(payload, "postId"), text(payload, "authorId"), !eventType.endsWith("deleted"));
        else if ("content.story.created".equals(eventType) || "content.story.deleted".equals(eventType)) upsert(TargetType.STORY, required(payload, "storyId"), text(payload, "authorId"), eventType.endsWith("created"));
        else if (eventType.startsWith("event.")) upsert(TargetType.EVENT, required(payload, "eventId"), text(payload, "organizerUserId"), !"event.cancelled".equals(eventType));
        else if (eventType.startsWith("place.suggestion.")) upsert(TargetType.PLACE_SUGGESTION, required(payload, "suggestionId"), text(payload, "userId"), !eventType.endsWith("rejected"));
        else if (eventType.startsWith("place.")) upsert(TargetType.PLACE, required(payload, "placeId"), null, !eventType.endsWith("deleted"));
        ProcessedEventEntity receipt = new ProcessedEventEntity(); receipt.setEventId(eventId); receipt.setEventType(eventType); receipt.setProcessedAt(Instant.now()); processed.save(receipt);
    }
    private void upsert(TargetType type, String id, String owner, boolean available) { ReportedTargetEntity entity = targets.findById(key(type, id)).orElseGet(ReportedTargetEntity::new); entity.setTargetKey(key(type,id)); entity.setTargetType(type.name()); entity.setTargetId(id); entity.setOwnerId(blank(owner) ? null : owner); entity.setAvailable(available); entity.setUpdatedAt(Instant.now()); targets.save(entity); }
    private String key(TargetType type, String id) { return type.name() + ":" + id; }
    private String required(JsonNode n, String f) { String v = text(n,f); if (blank(v)) throw new IllegalArgumentException(f+" is required"); return v; }
    private String text(JsonNode n, String f) { JsonNode v=n.get(f); return v==null||v.isNull()?null:v.asText(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
