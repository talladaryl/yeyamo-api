package com.yeyamo_mobile.api.analytics_service.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
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
import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;
import com.yeyamo_mobile.api.analytics_service.repository.AnalyticsEventLogRepository;
import com.yeyamo_mobile.api.analytics_service.repository.KpiHistoryRepository;

@Service
public class AnalyticsIngestionService {

    private final ObjectMapper objectMapper;
    private final AnalyticsEventLogRepository eventLogRepository;
    private final KpiHistoryRepository kpiHistoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String auditTopic;

    public AnalyticsIngestionService(
            ObjectMapper objectMapper,
            AnalyticsEventLogRepository eventLogRepository,
            KpiHistoryRepository kpiHistoryRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${yeyamo.kafka.topics.audit-events:audit-events}") String auditTopic
    ) {
        this.objectMapper = objectMapper;
        this.eventLogRepository = eventLogRepository;
        this.kpiHistoryRepository = kpiHistoryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.auditTopic = auditTopic;
    }

    public void process(String rawPayload) {
        String eventType = "unknown";
        String correlationId = null;
        UUID eventId = UUID.randomUUID();
        try {
            JsonNode event = objectMapper.readTree(rawPayload);
            eventId = uuid(event, "eventId", eventId);
            eventType = text(event, "eventType", eventType);
            correlationId = text(event, "correlationId", null);
            validateEnvelope(event, eventId, eventType);
            if (eventLogRepository.existsByEventId(eventId)) {
                return;
            }

            saveKpiSnapshot(eventId, eventType, event);
            saveEventLog(eventId, eventType, AnalyticsEventStatus.SUCCESS);
        } catch (Exception exception) {
            saveEventLog(eventId, eventType, AnalyticsEventStatus.FAILED);
            publishAuditFailure(eventType, correlationId, exception.getMessage());
        }
    }

    private void validateEnvelope(JsonNode event, UUID eventId, String eventType) {
        if (eventId == null || eventType == null || "unknown".equals(eventType)) {
            throw new IllegalArgumentException("Invalid domain event identity");
        }
        if (event.path("eventVersion").asInt(0) < 1) {
            throw new IllegalArgumentException("Unsupported domain event version");
        }
        if (text(event, "producer", null) == null || event.path("payload").isMissingNode()) {
            throw new IllegalArgumentException("Incomplete domain event envelope");
        }
    }

    private void saveKpiSnapshot(UUID eventId, String eventType, JsonNode event) {
        KpiHistory kpi = new KpiHistory();
        kpi.setKpiName(resolveKpiName(eventType));
        kpi.setEventType(eventType);
        kpi.setCalculatedAt(LocalDateTime.now());
        kpi.setEntityId(uuid(event.path("payload"), "id",
                uuid(event.path("payload"), "placeId",
                        uuid(event.path("payload"), "eventId", eventId))));
        kpi.setEntityType(resolveEntityType(eventType));
        kpi.setKpiValue(Map.of(
                "count", 1,
                "sourceEventId", eventId.toString(),
                "eventType", eventType
        ));
        kpiHistoryRepository.save(kpi);
    }

    private void saveEventLog(UUID eventId, String eventType, AnalyticsEventStatus status) {
        AnalyticsEventLog log = new AnalyticsEventLog();
        log.setEventId(eventId);
        log.setEventType(eventType);
        log.setProcessedAt(LocalDateTime.now());
        log.setStatus(status);
        eventLogRepository.save(log);
    }

    private void publishAuditFailure(String eventType, String correlationId, String errorMessage) {
        try {
            AuditEvent auditEvent = AuditEvent.failedAnalyticsEvent(eventType, correlationId, errorMessage);
            kafkaTemplate.send(auditTopic, auditEvent.eventId().toString(), objectMapper.writeValueAsString(auditEvent));
        } catch (JsonProcessingException ignored) {
            // The failure is already captured in analytics_event_logs.
        }
    }

    private String resolveKpiName(String eventType) {
        if (eventType == null) {
            return "events_processed";
        }
        if (eventType.contains("user")) {
            return "users_activity";
        }
        if (eventType.contains("place")) {
            return "places_activity";
        }
        if (eventType.contains("booking")) {
            return "bookings_activity";
        }
        if (eventType.contains("partner")) {
            return "partners_activity";
        }
        return "events_processed";
    }

    private String resolveEntityType(String eventType) {
        if (eventType == null || !eventType.contains(".")) {
            return "EVENT";
        }
        return eventType.substring(0, eventType.indexOf('.')).toUpperCase();
    }

    private UUID uuid(JsonNode node, String field, UUID defaultValue) {
        String value = text(node, field, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return defaultValue;
        }
    }

    private String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? defaultValue : value.asText();
    }
}
