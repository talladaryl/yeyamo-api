package com.yeyamo_mobile.api.feed_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.feed_service.application.FeedProjectionCommandService;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedPost;

class FeedEventConsumerTests {

    @Test
    void publicPublishedPostIsProjectedAndReplayIsIgnored() throws Exception {
        UUID event = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(event)).thenReturn(false, true);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);

        String json = postEvent(event, post, "PUBLISHED", "PUBLIC", "2026-07-13T12:00:00Z");
        consumer.content(json);
        consumer.content(json);

        ArgumentCaptor<FeedPost> postCaptor = ArgumentCaptor.forClass(FeedPost.class);
        verify(commands, times(1)).content(eq("content.post.published"), postCaptor.capture(), eq(event.toString()));
        assertTrue(postCaptor.getValue().available());
        verify(receipts, times(1)).save(any());
    }

    @Test
    void automaticEventPostReachesTheNormalFeedProjection() throws Exception {
        UUID outboxEvent = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        UUID referencedEvent = UUID.randomUUID();
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(outboxEvent)).thenReturn(false);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);

        consumer.content(postEvent(outboxEvent, post, "PUBLISHED", "PUBLIC", "2026-07-13T12:00:00Z", "EVENT", referencedEvent.toString()));

        ArgumentCaptor<FeedPost> projected = ArgumentCaptor.forClass(FeedPost.class);
        verify(commands).content(eq("content.post.published"), projected.capture(), eq(outboxEvent.toString()));
        assertTrue(projected.getValue().available());
        assertEquals("EVENT", projected.getValue().referenceType());
        assertEquals(referencedEvent.toString(), projected.getValue().referenceId());
    }

    @Test
    void nonPublicOrNonPublishedPostsRemainUnavailableToFeedQueries() throws Exception {
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);

        UUID privateEvent = UUID.randomUUID();
        UUID draftEvent = UUID.randomUUID();
        when(receipts.existsById(privateEvent)).thenReturn(false);
        when(receipts.existsById(draftEvent)).thenReturn(false);

        consumer.content(postEvent(privateEvent, UUID.randomUUID(), "PUBLISHED", "PRIVATE", "2026-07-13T12:00:00Z"));
        consumer.content(postEvent(draftEvent, UUID.randomUUID(), "DRAFT", "PUBLIC", null));

        ArgumentCaptor<FeedPost> postCaptor = ArgumentCaptor.forClass(FeedPost.class);
        verify(commands, times(2)).content(eq("content.post.published"), postCaptor.capture(), any());
        assertFalse(postCaptor.getAllValues().get(0).available());
        assertFalse(postCaptor.getAllValues().get(1).available());
    }

    @Test
    void storyEventIsAcknowledgedWithoutCreatingAFeedProjectionAndReplayIsIgnored() {
        UUID event = UUID.randomUUID();
        UUID story = UUID.randomUUID();
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(event)).thenReturn(false, true);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);

        String json = ("{\"eventId\":\"%s\",\"eventType\":\"content.story.created\",\"eventVersion\":1,"
                + "\"producer\":\"content-service\",\"occurredAt\":\"2026-07-13T12:00:00Z\","
                + "\"payload\":{\"storyId\":\"%s\",\"authorId\":\"a1\",\"mediaId\":\"%s\"}}").formatted(event, story, UUID.randomUUID());

        assertDoesNotThrow(() -> consumer.content(json));
        assertDoesNotThrow(() -> consumer.content(json));

        verifyNoInteractions(commands);
        verify(receipts, times(1)).save(any());
    }

    @Test
    void validButUnsupportedContentEventIsAcknowledged() {
        UUID event = UUID.randomUUID();
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(event)).thenReturn(false);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);

        String json = ("{\"eventId\":\"%s\",\"eventType\":\"content.asset.transcoded\",\"eventVersion\":1,"
                + "\"producer\":\"content-service\",\"occurredAt\":\"2026-07-13T12:00:00Z\",\"payload\":{}}").formatted(event);

        assertDoesNotThrow(() -> consumer.content(json));

        verifyNoInteractions(commands);
        verify(receipts).save(any());
    }

    @Test
    void unknownPostNamespaceEventIsAcknowledgedWithoutReadingPostPayload() {
        UUID event = UUID.randomUUID();
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(event)).thenReturn(false);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);

        String json = ("{\"eventId\":\"%s\",\"eventType\":\"content.post.future_event\",\"eventVersion\":1,"
                + "\"producer\":\"content-service\",\"occurredAt\":\"2026-07-13T12:00:00Z\",\"payload\":{}}").formatted(event);

        assertDoesNotThrow(() -> consumer.content(json));

        verifyNoInteractions(commands);
        verify(receipts).save(any());
    }

    @Test
    void interactionEventReplayIsIgnored() throws Exception {
        UUID event = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(event)).thenReturn(false, true);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);
        String json = ("{\"eventId\":\"%s\",\"eventType\":\"interaction.like.added\",\"eventVersion\":1,"
                + "\"producer\":\"interaction-service\",\"actorId\":\"u1\",\"payload\":{\"postId\":\"%s\",\"userId\":\"u1\"}}").formatted(event, post);

        consumer.interaction(json);
        consumer.interaction(json);

        verify(commands, times(1)).interaction("interaction.like.added", post, "u1", event.toString());
    }

    private String postEvent(UUID event, UUID post, String status, String visibility, String publishedAt) {
        return postEvent(event, post, status, visibility, publishedAt, "NONE", null);
    }

    private String postEvent(UUID event, UUID post, String status, String visibility, String publishedAt, String referenceType, String referenceId) {
        String publishedField = publishedAt == null ? "" : ",\"publishedAt\":\"" + publishedAt + "\"";
        String referenceIdField = referenceId == null ? "" : ",\"referenceId\":\"" + referenceId + "\"";
        return ("{\"eventId\":\"%s\",\"eventType\":\"content.post.published\",\"eventVersion\":1,"
                + "\"producer\":\"content-service\",\"occurredAt\":\"2026-07-13T12:00:00Z\","
                + "\"payload\":{\"postId\":\"%s\",\"authorId\":\"a1\",\"status\":\"%s\",\"visibility\":\"%s\",\"referenceType\":\"%s\"%s%s}}").formatted(
                        event, post, status, visibility, referenceType, referenceIdField, publishedField);
    }
}
