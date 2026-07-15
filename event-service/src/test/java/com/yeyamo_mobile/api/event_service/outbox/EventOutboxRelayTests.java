package com.yeyamo_mobile.api.event_service.outbox;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import java.time.Instant;import java.util.*;import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;import org.springframework.kafka.core.KafkaTemplate;

class EventOutboxRelayTests {
 @Test void marksAMessagePublishedOnlyAfterKafkaAcknowledgesIt() throws Exception{
  EventOutboxRepository repository=mock(EventOutboxRepository.class);KafkaTemplate<String,String> kafka=mockKafka();
  EventOutboxMessage message=new EventOutboxMessage();message.setId(UUID.randomUUID());message.setAggregateId("event-1");message.setEventType("event.created");message.setPayload("{}");message.setOccurredAt(Instant.now());
  when(repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()).thenReturn(List.of(message));when(kafka.send("event.events","event-1","{}")).thenReturn(CompletableFuture.completedFuture(null));
  new EventOutboxRelay(repository,kafka,"event.events").publishPending();
  assertNotNull(message.getPublishedAt());assertEquals(0,message.getAttempts());verify(repository).save(message);
 }
 @SuppressWarnings("unchecked") private KafkaTemplate<String,String> mockKafka(){return mock(KafkaTemplate.class);}
}
