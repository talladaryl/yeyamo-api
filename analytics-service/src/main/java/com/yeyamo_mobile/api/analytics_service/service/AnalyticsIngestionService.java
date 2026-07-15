package com.yeyamo_mobile.api.analytics_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.analytics_service.enums.AnalyticsEventStatus;
import com.yeyamo_mobile.api.analytics_service.event.AuditEvent;
import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;
import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.repository.AnalyticsEventLogRepository;
import com.yeyamo_mobile.api.analytics_service.service.projection.AnalyticsProjection;

@Service
public class AnalyticsIngestionService {

    private final ObjectMapper objectMapper;
    private final AnalyticsEventLogRepository eventLogRepository;
    private final List<AnalyticsProjection> projections;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String auditTopic;

    public AnalyticsIngestionService(
            ObjectMapper objectMapper,
            AnalyticsEventLogRepository eventLogRepository,
            List<AnalyticsProjection> projections,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${yeyamo.kafka.topics.audit-events:audit-events}") String auditTopic
    ) {
        this.objectMapper = objectMapper;
        this.eventLogRepository = eventLogRepository;
        this.projections = projections.stream().sorted(Comparator.comparingInt(AnalyticsProjection::order)).toList();
        this.kafkaTemplate = kafkaTemplate;
        this.auditTopic = auditTopic;
    }

    public void process(String rawPayload) {
        String eventType = "unknown";
        String correlationId = null;
        UUID eventId = UUID.randomUUID();
        try {
            JsonNode rawEvent = objectMapper.readTree(rawPayload);
            eventId = requiredUuid(rawEvent, "eventId");
            eventType = requiredText(rawEvent, "eventType");
            correlationId = text(rawEvent, "correlationId", eventId.toString());
            AnalyticsDomainEvent event = envelope(rawEvent, eventId, eventType, correlationId);
            if (eventLogRepository.findByEventId(eventId)
                    .filter(log -> log.getStatus() == AnalyticsEventStatus.SUCCESS).isPresent()) {
                return;
            }

            for (AnalyticsProjection projection : projections) {
                if (projection.supports(event)) {
                    projection.project(event);
                }
            }
            saveEventLog(eventId, eventType, AnalyticsEventStatus.SUCCESS);
        } catch (Exception exception) {
            saveEventLog(eventId, eventType, AnalyticsEventStatus.FAILED);
            publishAuditFailure(eventType, correlationId, exception.getMessage());
            throw new AnalyticsIngestionException("Analytics event processing failed", exception);
        }
    }

    private AnalyticsDomainEvent envelope(JsonNode event, UUID eventId, String eventType, String correlationId) {
        int version = event.path("eventVersion").asInt(0);
        if (version != 1) {
            throw new IllegalArgumentException("Unsupported domain event version");
        }
        String producer = requiredText(event, "producer");
        JsonNode payload = event.path("payload");
        if (!payload.isObject()) {
            throw new IllegalArgumentException("Incomplete domain event envelope");
        }
        Instant occurredAt;
        try {
            occurredAt = Instant.parse(requiredText(event, "occurredAt"));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("occurredAt must be an ISO-8601 instant", exception);
        }
        return new AnalyticsDomainEvent(eventId, eventType, version, occurredAt, producer,
                correlationId, text(event, "actorId", null), payload);
    }

    private void saveEventLog(UUID eventId, String eventType, AnalyticsEventStatus status) {
        AnalyticsEventLog log = eventLogRepository.findByEventId(eventId).orElseGet(AnalyticsEventLog::new);
        log.setId(eventId);
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setProcessedAt(LocalDateTime.now());
        log.setStatus(status);
        eventLogRepository.save(log);
    }

    static final class AnalyticsIngestionException extends RuntimeException {
        AnalyticsIngestionException(String message, Throwable cause) { super(message, cause); }
    }

    private void publishAuditFailure(String eventType, String correlationId, String errorMessage) {
        try {
            AuditEvent auditEvent = AuditEvent.failedAnalyticsEvent(eventType, correlationId, errorMessage);
            kafkaTemplate.send(auditTopic, auditEvent.eventId().toString(), objectMapper.writeValueAsString(auditEvent));
        } catch (JsonProcessingException ignored) {
            // The failure is already captured in analytics_event_logs.
        }
    }

    private UUID requiredUuid(JsonNode node, String field) {
        try {
            return UUID.fromString(requiredText(node, field));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " must be a UUID", exception);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = text(node, field, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? defaultValue : value.asText();
    }
}
