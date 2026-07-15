package com.yeyamo_mobile.api.analytics_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.analytics_service.enums.AnalyticsEventStatus;
import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;
import com.yeyamo_mobile.api.analytics_service.repository.AnalyticsEventLogRepository;
import com.yeyamo_mobile.api.analytics_service.service.projection.AnalyticsProjection;

class AnalyticsIngestionServiceTests {

    @Test
    void ignoresAnAlreadySuccessfulEvent() {
        UUID id = UUID.randomUUID();
        AnalyticsEventLog existing = new AnalyticsEventLog();
        existing.setEventId(id);
        existing.setStatus(AnalyticsEventStatus.SUCCESS);
        AnalyticsEventLogRepository logs = mock(AnalyticsEventLogRepository.class);
        AnalyticsProjection projection = mock(AnalyticsProjection.class);
        when(logs.findByEventId(id)).thenReturn(Optional.of(existing));
        AnalyticsIngestionService service = service(logs, List.of(projection), mockKafka());

        service.process(envelope(id));

        verifyNoInteractions(projection);
        verify(logs, never()).save(any());
    }

    @Test
    void appliesSupportedStrategiesInOrderAndStoresTheSuccessMarker() {
        UUID id = UUID.randomUUID();
        AnalyticsEventLogRepository logs = mock(AnalyticsEventLogRepository.class);
        AnalyticsProjection projection = mock(AnalyticsProjection.class);
        when(projection.order()).thenReturn(10);
        when(projection.supports(any())).thenReturn(true);
        when(logs.findByEventId(id)).thenReturn(Optional.empty());
        AnalyticsIngestionService service = service(logs, List.of(projection), mockKafka());

        service.process(envelope(id));

        verify(projection).project(argThat(event -> event.eventId().equals(id)
                && event.eventType().equals("content.post.published")));
        verify(logs).save(argThat(log -> id.equals(log.getId())
                && log.getStatus() == AnalyticsEventStatus.SUCCESS));
    }

    @Test
    void invalidEnvelopeIsMarkedFailedAndPublishedToAudit() {
        AnalyticsEventLogRepository logs = mock(AnalyticsEventLogRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(eq("audit.events"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        AnalyticsIngestionService service = service(logs, List.of(), kafka);

        assertThrows(AnalyticsIngestionService.AnalyticsIngestionException.class,
                () -> service.process("{\"eventType\":\"content.post.published\"}"));

        verify(logs).save(argThat(log -> log.getStatus() == AnalyticsEventStatus.FAILED));
        verify(kafka).send(eq("audit.events"), any(), any());
    }

    private AnalyticsIngestionService service(AnalyticsEventLogRepository logs,
            List<AnalyticsProjection> projections, KafkaTemplate<String, String> kafka) {
        return new AnalyticsIngestionService(new ObjectMapper().findAndRegisterModules(), logs,
                projections, kafka, "audit.events");
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> mockKafka() {
        return mock(KafkaTemplate.class);
    }

    private String envelope(UUID id) {
        return "{\"eventId\":\"" + id
                + "\",\"eventType\":\"content.post.published\",\"eventVersion\":1,"
                + "\"occurredAt\":\"" + Instant.parse("2026-07-15T10:00:00Z")
                + "\",\"producer\":\"content-service\",\"actorId\":\"user-1\","
                + "\"payload\":{\"postId\":\"" + UUID.randomUUID() + "\",\"authorId\":\"user-1\"}}";
    }
}
