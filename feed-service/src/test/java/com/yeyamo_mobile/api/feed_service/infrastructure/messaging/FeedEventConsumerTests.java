package com.yeyamo_mobile.api.feed_service.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.feed_service.application.FeedProjectionCommandService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeedEventConsumerTests {
 @Test void contentEventReplayIsIgnored() throws Exception {
  UUID event=UUID.randomUUID(),post=UUID.randomUUID(); FeedProjectionCommandService commands=mock(FeedProjectionCommandService.class); ProcessedEventRepository receipts=mock(ProcessedEventRepository.class); when(receipts.existsById(event)).thenReturn(false,true);
  FeedEventConsumer consumer=new FeedEventConsumer(new ObjectMapper(),commands,receipts);
  String json=("{\"eventId\":\"%s\",\"eventType\":\"content.post.published\",\"eventVersion\":1,\"producer\":\"content-service\",\"occurredAt\":\"2026-07-13T12:00:00Z\",\"payload\":{\"postId\":\"%s\",\"authorId\":\"a1\",\"status\":\"PUBLISHED\",\"visibility\":\"PUBLIC\",\"publishedAt\":\"2026-07-13T12:00:00Z\"}}").formatted(event,post);
  consumer.content(json); consumer.content(json);
  verify(commands,times(1)).content(eq("content.post.published"),any(),eq(event.toString())); verify(receipts,times(1)).save(any());
 }
 @Test void interactionEventReplayIsIgnored() throws Exception {
  UUID event=UUID.randomUUID(),post=UUID.randomUUID(); FeedProjectionCommandService commands=mock(FeedProjectionCommandService.class); ProcessedEventRepository receipts=mock(ProcessedEventRepository.class); when(receipts.existsById(event)).thenReturn(false,true);
  FeedEventConsumer consumer=new FeedEventConsumer(new ObjectMapper(),commands,receipts);
  String json=("{\"eventId\":\"%s\",\"eventType\":\"interaction.like.added\",\"eventVersion\":1,\"producer\":\"interaction-service\",\"actorId\":\"u1\",\"payload\":{\"postId\":\"%s\",\"userId\":\"u1\"}}").formatted(event,post);
  consumer.interaction(json); consumer.interaction(json); verify(commands,times(1)).interaction("interaction.like.added",post,"u1",event.toString());
 }
 @Test void cultureEventProjectsStructuredLink() throws Exception {
  UUID event=UUID.randomUUID(),content=UUID.randomUUID(); FeedProjectionCommandService commands=mock(FeedProjectionCommandService.class); ProcessedEventRepository receipts=mock(ProcessedEventRepository.class); when(receipts.existsById(event)).thenReturn(false);
  FeedEventConsumer consumer=new FeedEventConsumer(new ObjectMapper(),commands,receipts);
  String json=("{\"eventId\":\"%s\",\"eventType\":\"CultureContentPublished\",\"eventVersion\":1,\"producer\":\"culture-service\",\"payload\":{\"contentId\":\"%s\",\"type\":\"RECIPE\",\"title\":\"Ndole\",\"isActive\":true}}").formatted(event,content);
  consumer.culture(json); verify(commands).culture(argThat(link->link.contentId().equals(content)&&link.type().equals("RECIPE")&&link.active()),eq(event.toString()));
 }
}
