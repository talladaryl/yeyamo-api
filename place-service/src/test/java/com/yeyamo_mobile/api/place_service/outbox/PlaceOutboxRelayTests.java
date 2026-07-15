package com.yeyamo_mobile.api.place_service.outbox;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import java.time.Instant;import java.util.*;import java.util.concurrent.CompletableFuture;import org.junit.jupiter.api.Test;import org.springframework.kafka.core.KafkaTemplate;
class PlaceOutboxRelayTests{
 @Test void marksLegacyPlaceEventPublishedAfterKafkaAck(){PlaceOutboxRepository repository=mock(PlaceOutboxRepository.class);KafkaTemplate<String,String> kafka=mockKafka();PlaceOutboxMessage message=new PlaceOutboxMessage();message.setId(UUID.randomUUID());message.setAggregateId("place-1");message.setEventType("place.updated");message.setPayload("{}");message.setOccurredAt(Instant.now());when(repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()).thenReturn(List.of(message));when(kafka.send("place.events","place-1","{}")).thenReturn(CompletableFuture.completedFuture(null));new PlaceOutboxRelay(repository,kafka,"place.events").publishPending();assertNotNull(message.getPublishedAt());verify(repository).save(message);}
 @SuppressWarnings("unchecked")private KafkaTemplate<String,String> mockKafka(){return mock(KafkaTemplate.class);}
}
