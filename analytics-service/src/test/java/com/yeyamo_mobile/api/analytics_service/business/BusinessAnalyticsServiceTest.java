package com.yeyamo_mobile.api.analytics_service.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class BusinessAnalyticsServiceTest {
    private final AnalyticsInboxRepository inbox = mock(AnalyticsInboxRepository.class);
    private final StoredAnalyticsEventRepository events = mock(StoredAnalyticsEventRepository.class);
    private final ReachFingerprintRepository reach = mock(ReachFingerprintRepository.class);
    private final DailyAggregateRepository aggregates = mock(DailyAggregateRepository.class);
    private final BusinessAnalyticsService service = new BusinessAnalyticsService(
        new ObjectMapper(), inbox, events, reach, aggregates,
        new SimpleMeterRegistry(), 5);

    @Test
    void duplicateKafkaEventDoesNotTouchProjections() {
        UUID id = UUID.randomUUID();
        when(inbox.existsById(id)).thenReturn(true);

        service.ingest(envelope(id, "ad.click.recorded",
            "{\"campaignId\":\"c1\",\"cost\":12}"));

        verifyNoInteractions(events, reach, aggregates);
        verify(inbox, never()).save(any());
    }

    @Test
    void calculatesCtrCpmCpcAndCpaWithoutDivisionByZero() {
        DailyAggregate row = aggregate("TOTAL", "ALL");
        row.impressions = 1000;
        row.clicks = 50;
        row.conversions = 10;
        row.spend = new BigDecimal("2500");
        when(aggregates.findByPartnerIdAndScopeTypeAndScopeIdAndStatDateBetween(
            eq("p1"), eq("CAMPAIGN"), eq("c1"), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(row)));

        var metrics = service.campaign("p1", "c1", LocalDate.now().minusDays(1),
            LocalDate.now(), PageRequest.of(0, 20)).getContent().getFirst();

        assertEquals(new BigDecimal("5.0000"), metrics.ctr());
        assertEquals(new BigDecimal("20.0000"), metrics.conversionRate());
        assertEquals(new BigDecimal("2500.0000"), metrics.cpm());
        assertEquals(new BigDecimal("50.0000"), metrics.cpc());
        assertEquals(new BigDecimal("250.0000"), metrics.cpa());
    }

    @Test
    void suppressesSmallSensitiveSegmentsAndStaffIdentity() {
        DailyAggregate row = aggregate("STAFF", "raw-staff-id");
        row.scans = 4;
        when(aggregates.findByPartnerIdAndScopeTypeAndScopeIdAndStatDateBetween(
            eq("p1"), eq("TICKET_EVENT"), eq("event-1"), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(row)));

        var metrics = service.ticketing("p1", "event-1", LocalDate.now().minusDays(1),
            LocalDate.now(), PageRequest.of(0, 20)).getContent().getFirst();

        assertTrue(metrics.suppressed());
        assertEquals("SUPPRESSED", metrics.dimensionValue());
        assertEquals(0, metrics.scans());
    }

    @Test
    void missingDataReturnsAnEmptyPage() {
        when(aggregates.findByPartnerIdAndScopeTypeAndScopeIdAndStatDateBetween(
            any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
        assertTrue(service.campaign("p1", "unknown", LocalDate.now(), LocalDate.now(),
            PageRequest.of(0, 20)).isEmpty());
    }

    @Test
    void lateEventIsAggregatedOnItsUtcOccurrenceDate() {
        UUID id = UUID.randomUUID();
        when(inbox.existsById(id)).thenReturn(false);
        when(aggregates.locked(any(), any(), any(), any(), any()))
            .thenReturn(Optional.empty());

        service.ingest("""
            {"eventId":"%s","eventType":"ad.click.recorded","eventVersion":1,
             "occurredAt":"2025-12-31T23:30:00Z","producer":"ads",
             "payload":{"campaignId":"late-campaign","partnerId":"p1","cost":1}}
            """.formatted(id));

        ArgumentCaptor<DailyAggregate> rows =
            ArgumentCaptor.forClass(DailyAggregate.class);
        verify(aggregates).save(rows.capture());
        assertEquals(LocalDate.of(2025, 12, 31), rows.getValue().statDate);
    }

    @Test
    void rebuildClearsDerivedStateAndReplaysStoredEvents() {
        StoredAnalyticsEvent event = new StoredAnalyticsEvent();
        event.eventId = UUID.randomUUID();
        event.eventType = "ad.click.recorded";
        event.occurredAt = java.time.Instant.parse("2026-01-01T00:00:00Z");
        event.partnerId = "p1";
        event.campaignId = "c1";
        event.payload = envelope(event.eventId, event.eventType,
            "{\"campaignId\":\"c1\",\"partnerId\":\"p1\"}");
        when(events.findAllByOrderByOccurredAtAscEventIdAsc())
            .thenReturn(List.of(event));
        when(aggregates.locked(any(), any(), any(), any(), any()))
            .thenReturn(Optional.empty());

        assertEquals(1, service.rebuild());

        verify(aggregates).clearAll();
        verify(reach).deleteAllInBatch();
        verify(aggregates).save(any());
    }

    @Test
    void calculationsRemainStableForSimulatedLargeVolumes() {
        DailyAggregate row = aggregate("TOTAL", "ALL");
        row.impressions = 50_000_000;
        row.clicks = 2_500_000;
        row.conversions = 125_000;
        row.spend = new BigDecimal("999999999999.99");
        when(aggregates.findByPartnerIdAndScopeTypeAndScopeIdAndStatDateBetween(
            any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(row)));

        var value = service.campaign("p1", "c1", LocalDate.now(),
            LocalDate.now(), PageRequest.of(0, 20)).getContent().getFirst();

        assertEquals(new BigDecimal("5.0000"), value.ctr());
        assertEquals(new BigDecimal("5.0000"), value.conversionRate());
        assertTrue(value.cpm().signum() > 0);
    }

    private DailyAggregate aggregate(String dimension, String value) {
        DailyAggregate row = new DailyAggregate();
        row.id = UUID.randomUUID();
        row.statDate = LocalDate.now();
        row.scopeType = "CAMPAIGN";
        row.scopeId = "c1";
        row.dimensionType = dimension;
        row.dimensionValue = value;
        return row;
    }

    private String envelope(UUID id, String type, String payload) {
        return """
            {"eventId":"%s","eventType":"%s","eventVersion":1,
             "occurredAt":"2026-07-26T10:00:00Z","producer":"test",
             "payload":%s}
            """.formatted(id, type, payload);
    }
}
