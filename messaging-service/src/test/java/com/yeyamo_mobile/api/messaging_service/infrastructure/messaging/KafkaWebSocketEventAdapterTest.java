package com.yeyamo_mobile.api.messaging_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

class KafkaWebSocketEventAdapterTest {

    @Test
    void publishesTheVersionedKafkaEnvelopeAndPrivateWebSocketMessages() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        SimpMessagingTemplate websocket = mock(SimpMessagingTemplate.class);
        when(kafka.send(eq("messaging.events"), eq("conversation-1"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var adapter = new KafkaWebSocketEventAdapter(kafka, websocket, mapper, "messaging.events");

        adapter.publish("messaging.message.sent", "conversation-1", "alice", "corr-1",
                List.of("bob", "carol"), Map.of("messageId", "message-1"), Map.of("body", "Bonjour"));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("messaging.events"), eq("conversation-1"), json.capture());
        var event = mapper.readTree(json.getValue());
        assertEquals(1, event.path("eventVersion").asInt());
        assertEquals("messaging-service", event.path("producer").asText());
        assertEquals("message-1", event.path("payload").path("messageId").asText());
        assertTrue(event.path("payload").path("recipientIds").isArray());
        verify(websocket).convertAndSendToUser("bob", "/queue/messaging", Map.of("body", "Bonjour"));
        verify(websocket).convertAndSendToUser("carol", "/queue/messaging", Map.of("body", "Bonjour"));
    }
}
