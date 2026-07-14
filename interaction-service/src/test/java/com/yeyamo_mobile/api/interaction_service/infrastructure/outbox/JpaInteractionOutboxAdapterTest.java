package com.yeyamo_mobile.api.interaction_service.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

class JpaInteractionOutboxAdapterTest {

    @Test
    void createsVersionedEventEnvelopeForKafka() throws Exception {
        InteractionOutboxRepository repository = mock(InteractionOutboxRepository.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        JpaInteractionOutboxAdapter adapter = new JpaInteractionOutboxAdapter(repository, mapper);
        UUID postId = UUID.randomUUID();

        adapter.append("interaction.like.added", "post", postId.toString(), "user-1", "corr-1",
                Map.of("postId", postId, "userId", "user-1"));

        ArgumentCaptor<InteractionOutboxEvent> captor = ArgumentCaptor.forClass(InteractionOutboxEvent.class);
        verify(repository).save(captor.capture());
        InteractionOutboxEvent stored = captor.getValue();
        var json = mapper.readTree(stored.getPayload());
        assertEquals("interaction.like.added", stored.getEventType());
        assertEquals(postId.toString(), stored.getAggregateId());
        assertEquals("interaction-service", json.get("producer").asText());
        assertEquals(1, json.get("eventVersion").asInt());
        assertEquals("corr-1", json.get("correlationId").asText());
        assertEquals(postId.toString(), json.get("payload").get("postId").asText());
    }
}
