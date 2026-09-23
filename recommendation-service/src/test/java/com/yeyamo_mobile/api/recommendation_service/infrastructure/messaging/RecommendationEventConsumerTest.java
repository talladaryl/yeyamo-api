package com.yeyamo_mobile.api.recommendation_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.recommendation_service.application.RecommendationProjectionService;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePlanAvailabilityService;
import com.yeyamo_mobile.api.recommendation_service.domain.Candidate;

class RecommendationEventConsumerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void projectsPublishedCatalogAsset() throws Exception {
        var service = mock(RecommendationProjectionService.class); var receipts = mock(ProcessedEventRepository.class);
        var consumer = new RecommendationEventConsumer(mapper, service, receipts);
        consumer.catalog(event("catalog.asset.status_changed", "{\"assetId\":\"1\",\"type\":\"PLACE\",\"name\":\"Plage\",\"status\":\"PUBLISHED\",\"latitude\":4,\"longitude\":9}"));
        var captor = ArgumentCaptor.forClass(Candidate.class); verify(service).candidate(captor.capture());
        assertTrue(captor.getValue().active()); assertEquals("catalog:1", captor.getValue().sourceId()); verify(receipts).save(any());
    }

    @Test void projectsPublishedEventAndMarksCancelledSnapshotUnavailable() throws Exception {
        var service = mock(RecommendationProjectionService.class); var receipts = mock(ProcessedEventRepository.class);
        var availability = mock(AdventurePlanAvailabilityService.class);
        var consumer = new RecommendationEventConsumer(mapper, service, receipts, availability);
        consumer.event(eventFrom("event-service", "event.published", "{\"eventId\":\"evt-1\",\"title\":\"Festival\",\"status\":\"PUBLISHED\",\"visibility\":\"PUBLIC\",\"countryCode\":\"CM\",\"startAt\":\"2026-10-06T10:00:00Z\",\"endAt\":\"2026-10-06T11:00:00Z\"}"));
        var captor = ArgumentCaptor.forClass(Candidate.class); verify(service).candidate(captor.capture());
        assertTrue(captor.getValue().active()); assertEquals("event:evt-1", captor.getValue().sourceId());
        consumer.event(eventFrom("event-service", "event.cancelled", "{\"eventId\":\"evt-1\",\"title\":\"Festival\",\"status\":\"CANCELLED\",\"visibility\":\"PUBLIC\"}"));
        verify(availability).markEventUnavailable("evt-1");
    }

    @Test void convertsInteractionIntoPopularityAndHistory() throws Exception {
        var service = mock(RecommendationProjectionService.class);
        var consumer = new RecommendationEventConsumer(mapper, service, mock(ProcessedEventRepository.class));
        consumer.interaction(event("interaction.favorite.added", "{\"postId\":\"42\",\"userId\":\"u1\"}"));
        verify(service).popularity("content:42", 3); verify(service).signal("u1", "content:42", 3);
    }

    @Test void projectsFeedbackWithoutTreatingItAsAFeedMetric() throws Exception {
        var service = mock(RecommendationProjectionService.class);
        var consumer = new RecommendationEventConsumer(mapper, service, mock(ProcessedEventRepository.class));
        consumer.interaction(event("interaction.feedback.updated", "{\"userId\":\"u1\",\"targetType\":\"EVENT\",\"targetId\":\"e1\",\"feedbackType\":\"NOT_INTERESTED\",\"previousFeedbackType\":\"INTERESTED\"}"));
        verify(service).feedback("u1", "EVENT", "e1", "NOT_INTERESTED", "INTERESTED");
        verify(service, never()).popularity(any(), anyDouble());
    }

    @Test void projectsUserPreferences() throws Exception {
        var service = mock(RecommendationProjectionService.class);
        var consumer = new RecommendationEventConsumer(mapper, service, mock(ProcessedEventRepository.class));
        consumer.user(event("profile.preferences_updated", "{\"authUserId\":\"u1\",\"preferredRegionId\":\"10\",\"language\":\"FRENCH\",\"locationSharingEnabled\":true}"));
        verify(service).preference("u1", "10", "FRENCH", true);
    }

    @Test void duplicateEventIsIgnored() throws Exception {
        UUID id = UUID.randomUUID(); var service = mock(RecommendationProjectionService.class); var receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(id)).thenReturn(true);
        new RecommendationEventConsumer(mapper, service, receipts).feed(event(id, "feed.metrics.updated", "{\"postId\":\"1\"}"));
        verifyNoInteractions(service); verify(receipts, never()).save(any());
    }

    @Test void replayedEventProjectionIsIgnored() throws Exception {
        UUID envelopeId = UUID.randomUUID();
        var service = mock(RecommendationProjectionService.class); var receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(envelopeId)).thenReturn(true);
        new RecommendationEventConsumer(mapper, service, receipts).event(eventFrom(envelopeId, "event-service", "event.updated",
                "{\"eventId\":\"evt-1\",\"title\":\"Festival\",\"status\":\"PUBLISHED\",\"visibility\":\"PUBLIC\"}"));
        verifyNoInteractions(service); verify(receipts, never()).save(any());
    }

    private String event(String type, String payload) { return event(UUID.randomUUID(), type, payload); }
    private String event(UUID id, String type, String payload) {
        return "{\"eventId\":\"" + id + "\",\"eventType\":\"" + type + "\",\"eventVersion\":1,\"payload\":" + payload + "}";
    }
    private String eventFrom(String producer, String type, String payload) {
        return eventFrom(UUID.randomUUID(), producer, type, payload);
    }
    private String eventFrom(UUID eventId, String producer, String type, String payload) {
        return "{\"eventId\":\"" + eventId + "\",\"producer\":\"" + producer + "\",\"eventType\":\"" + type + "\",\"eventVersion\":1,\"payload\":" + payload + "}";
    }
}
