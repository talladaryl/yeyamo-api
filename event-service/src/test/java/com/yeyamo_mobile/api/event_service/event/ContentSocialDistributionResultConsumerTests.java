package com.yeyamo_mobile.api.event_service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.event_service.enums.SocialDistributionStatus;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;
import com.yeyamo_mobile.api.event_service.repository.EventSocialDistributionReceiptRepository;

class ContentSocialDistributionResultConsumerTests {

    @Test
    void updatesEventWithPublishedFeedResultAndIgnoresReplay() throws Exception {
        Event event = new Event();
        UUID eventId = UUID.randomUUID();
        UUID resultEventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        event.setId(eventId);
        event.setPublishToFeed(true);
        event.setFeedDistributionStatus(SocialDistributionStatus.PENDING_MODERATION);
        EventRepository events = mock(EventRepository.class);
        EventSocialDistributionReceiptRepository receipts = mock(EventSocialDistributionReceiptRepository.class);
        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        when(receipts.existsById(resultEventId)).thenReturn(false, true);
        ContentSocialDistributionResultConsumer consumer = new ContentSocialDistributionResultConsumer(
                new ObjectMapper(), events, receipts);

        String raw = result(resultEventId, eventId, "FEED", "PUBLISHED", postId);
        consumer.consume(raw);
        consumer.consume(raw);

        assertEquals(SocialDistributionStatus.PUBLISHED, event.getFeedDistributionStatus());
        assertEquals(postId, event.getFeedPostId());
        verify(events).save(event);
        verify(receipts).save(any());
    }

    @Test
    void updatesStorySkippedNoMediaWithoutFalseSuccess() throws Exception {
        Event event = new Event();
        UUID eventId = UUID.randomUUID();
        event.setId(eventId);
        event.setPublishToStory(true);
        event.setStoryDistributionStatus(SocialDistributionStatus.PENDING_MODERATION);
        EventRepository events = mock(EventRepository.class);
        EventSocialDistributionReceiptRepository receipts = mock(EventSocialDistributionReceiptRepository.class);
        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        ContentSocialDistributionResultConsumer consumer = new ContentSocialDistributionResultConsumer(
                new ObjectMapper(), events, receipts);

        consumer.consume(result(UUID.randomUUID(), eventId, "STORY", "SKIPPED_NO_MEDIA", null));

        assertEquals(SocialDistributionStatus.SKIPPED_NO_MEDIA, event.getStoryDistributionStatus());
        assertEquals("NO_ELIGIBLE_COVER_MEDIA", event.getStoryDistributionReason());
    }

    @Test
    void delayedFailureCannotOverwriteFinalPublishedDistribution() throws Exception {
        Event event = new Event();
        UUID eventId = UUID.randomUUID();
        event.setId(eventId);
        event.setPublishToFeed(true);
        event.setFeedDistributionStatus(SocialDistributionStatus.PUBLISHED);
        EventRepository events = mock(EventRepository.class);
        EventSocialDistributionReceiptRepository receipts = mock(EventSocialDistributionReceiptRepository.class);
        when(events.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        ContentSocialDistributionResultConsumer consumer = new ContentSocialDistributionResultConsumer(
                new ObjectMapper(), events, receipts);

        consumer.consume(result(UUID.randomUUID(), eventId, "FEED", "FAILED", null));

        assertEquals(SocialDistributionStatus.PUBLISHED, event.getFeedDistributionStatus());
    }

    private String result(UUID resultId, UUID eventId, String target, String status, UUID contentId) {
        String content = contentId == null ? "" : ",\"contentId\":\"" + contentId + "\"";
        return ("{\"eventId\":\"%s\",\"eventType\":\"content.event_social.distribution.updated\",\"eventVersion\":1,"
                + "\"producer\":\"content-service\",\"occurredAt\":\"2026-09-17T08:00:00Z\","
                + "\"payload\":{\"eventId\":\"%s\",\"target\":\"%s\",\"status\":\"%s\","
                + "\"reason\":\"NO_ELIGIBLE_COVER_MEDIA\"%s}}").formatted(resultId, eventId, target, status, content);
    }
}
