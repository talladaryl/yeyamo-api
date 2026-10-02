package com.yeyamo_mobile.api.messaging_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.messaging_service.application.MessagingApplicationService;
import com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.ConversationView;
import com.yeyamo_mobile.api.messaging_service.application.port.MessagingEventPort;
import com.yeyamo_mobile.api.messaging_service.domain.ConversationType;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.OutingGroupLinkEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.OutingGroupLinkRepository;

@ExtendWith(MockitoExtension.class)
class OutingGroupConsumerTest {
    @Mock private MessagingApplicationService conversations;
    @Mock private OutingGroupLinkRepository links;
    @Mock private MessagingEventPort events;

    @Test
    void createsExactlyOneOwnerGroupForARepeatedPublicOutingEvent() throws Exception {
        UUID outingId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        var group = new ConversationView(groupId, ConversationType.GROUP, "Sortie", "owner-1", Instant.now(), Instant.now(), null, null, null, List.of());
        OutingGroupLinkEntity existing = OutingGroupLinkEntity.create(outingId, groupId, "owner-1");
        when(links.findById(outingId)).thenReturn(Optional.empty(), Optional.of(existing));
        when(conversations.create(eq("owner-1"), any(), eq("corr-1"))).thenReturn(group);

        String payload = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "producer", "event-service", "eventType", "event.published", "correlationId", "corr-1",
                "payload", java.util.Map.of("eventId", outingId.toString(), "organizerUserId", "owner-1", "title", "Sortie", "visibility", "PUBLIC")));
        OutingGroupConsumer consumer = new OutingGroupConsumer(new ObjectMapper(), conversations, links, events);

        consumer.consume(payload);
        consumer.consume(payload);

        ArgumentCaptor<OutingGroupLinkEntity> link = ArgumentCaptor.forClass(OutingGroupLinkEntity.class);
        verify(links).save(link.capture());
        assertEquals(outingId, link.getValue().getOutingId());
        assertEquals(groupId, link.getValue().getConversationId());
        verify(conversations, times(1)).create(eq("owner-1"), any(), eq("corr-1"));
    }

    @Test
    void addsTheConfirmedParticipantToTheExistingOutingGroup() throws Exception {
        UUID outingId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        OutingGroupLinkEntity existing = OutingGroupLinkEntity.create(outingId, groupId, "owner-1");
        when(links.findById(outingId)).thenReturn(Optional.of(existing));
        String payload = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "producer", "event-service", "eventType", "event.registration.created", "correlationId", "corr-2",
                "payload", java.util.Map.of("eventId", outingId.toString(), "registrationUserId", "participant-2")));

        new OutingGroupConsumer(new ObjectMapper(), conversations, links, events).consume(payload);

        verify(conversations).addMember("owner-1", groupId, "participant-2", "corr-2");
    }
}
