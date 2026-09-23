package com.yeyamo_mobile.api.event_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.EventVisibility;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.outbox.EventOutboxMessage;
import com.yeyamo_mobile.api.event_service.outbox.EventOutboxRepository;

class EventPublishedContractTest {

    @Test
    void publishedEventCarriesOnlyTheDataNeededForSocialContent() throws Exception {
        EventOutboxRepository outbox = mock(EventOutboxRepository.class);
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setOwnerUserId("organizer-1");
        event.setTitle("Sortie");
        event.setDescription("Description");
        event.setStartAt(Instant.parse("2026-10-05T09:00:00Z"));
        event.setEndAt(Instant.parse("2026-10-05T12:00:00Z"));
        event.setStatus(EventStatus.PUBLISHED);
        event.setVisibility(EventVisibility.PUBLIC);
        event.setCoverMediaId(UUID.randomUUID());
        event.setPublishToFeed(true);
        event.setPublishToStory(true);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        new EventPublisher(mapper, outbox).publishPublished(event, "corr-1", "moderator-1");

        ArgumentCaptor<EventOutboxMessage> message = ArgumentCaptor.forClass(EventOutboxMessage.class);
        verify(outbox).save(message.capture());
        var json = mapper.readTree(message.getValue().getPayload());
        assertEquals("event.published", json.path("eventType").asText());
        assertEquals("event-service", json.path("producer").asText());
        assertEquals(event.getId().toString(), json.path("payload").path("eventId").asText());
        assertEquals("organizer-1", json.path("payload").path("organizerUserId").asText());
        assertTrue(json.path("payload").path("publishToFeed").asBoolean());
        assertTrue(json.path("payload").path("publishToStory").asBoolean());
    }
}
