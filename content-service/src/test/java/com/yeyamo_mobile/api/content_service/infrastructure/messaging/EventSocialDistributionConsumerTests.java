package com.yeyamo_mobile.api.content_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.content_service.application.EventPublishedSocialCommand;
import com.yeyamo_mobile.api.content_service.application.EventSocialDistributionService;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.ContentProcessedEventReceiptRepository;

class EventSocialDistributionConsumerTests {

    @Test
    void consumesOnlyRealPublishedEventAndPassesPersistedIntent() throws Exception {
        EventSocialDistributionService service = mock(EventSocialDistributionService.class);
        ContentProcessedEventReceiptRepository receipts = mock(ContentProcessedEventReceiptRepository.class);
        UUID sourceId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(receipts.existsById(sourceId)).thenReturn(false);
        EventSocialDistributionConsumer consumer = new EventSocialDistributionConsumer(new ObjectMapper(), service, receipts);

        consumer.consume(event(sourceId, eventId, "event.published", true, false));

        ArgumentCaptor<EventPublishedSocialCommand> command = ArgumentCaptor.forClass(EventPublishedSocialCommand.class);
        verify(service).distribute(command.capture());
        org.junit.jupiter.api.Assertions.assertEquals(eventId, command.getValue().eventId());
        org.junit.jupiter.api.Assertions.assertTrue(command.getValue().publishToFeed());
        org.junit.jupiter.api.Assertions.assertFalse(command.getValue().publishToStory());
        org.junit.jupiter.api.Assertions.assertEquals("organizer-1", command.getValue().organizerUserId());
    }

    @Test
    void ignoresNonPublishedEventAndReceiptReplay() throws Exception {
        EventSocialDistributionService service = mock(EventSocialDistributionService.class);
        ContentProcessedEventReceiptRepository receipts = mock(ContentProcessedEventReceiptRepository.class);
        UUID sourceId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(receipts.existsById(sourceId)).thenReturn(true);
        EventSocialDistributionConsumer consumer = new EventSocialDistributionConsumer(new ObjectMapper(), service, receipts);

        assertDoesNotThrow(() -> consumer.consume(event(sourceId, eventId, "event.updated", true, true)));
        assertDoesNotThrow(() -> consumer.consume(event(sourceId, eventId, "event.published", true, true)));

        verifyNoInteractions(service);
    }

    private String event(UUID sourceId, UUID eventId, String type, boolean feed, boolean story) {
        return ("{\"eventId\":\"%s\",\"eventType\":\"%s\",\"eventVersion\":1,"
                + "\"producer\":\"event-service\",\"occurredAt\":\"2026-09-17T08:00:00Z\",\"correlationId\":\"corr-1\","
                + "\"payload\":{\"eventId\":\"%s\",\"organizerUserId\":\"organizer-1\",\"title\":\"Sortie\","
                + "\"description\":\"Description\",\"startAt\":\"2026-10-05T09:00:00Z\",\"endAt\":\"2026-10-05T12:00:00Z\","
                + "\"status\":\"PUBLISHED\",\"visibility\":\"PUBLIC\",\"publishToFeed\":%s,\"publishToStory\":%s}}").formatted(
                        sourceId, type, eventId, feed, story);
    }
}
