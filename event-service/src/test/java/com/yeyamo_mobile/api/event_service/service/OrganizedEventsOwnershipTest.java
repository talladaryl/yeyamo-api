package com.yeyamo_mobile.api.event_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.EventVisibility;
import com.yeyamo_mobile.api.event_service.event.EventPublisher;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.repository.EventRegistrationRepository;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;

class OrganizedEventsOwnershipTest {

    @Test
    void listsOnlyEventsOwnedByAuthenticatedActor() {
        EventRepository events = mock(EventRepository.class);
        Event event = eventOwnedBy("partner-user-a");
        PageRequest page = PageRequest.of(0, 20);
        when(events.findByOwnerUserIdOrderByCreatedAtDesc("partner-user-a", page))
                .thenReturn(new PageImpl<>(List.of(event), page, 1));

        EventService service = new EventService(
                events,
                mock(EventRegistrationRepository.class),
                mock(EventPublisher.class));

        var result = service.findOrganizedByUser("partner-user-a", page);

        assertThat(result.getContent()).singleElement()
                .satisfies(item -> assertThat(item.ownerUserId()).isEqualTo("partner-user-a"));
        verify(events).findByOwnerUserIdOrderByCreatedAtDesc("partner-user-a", page);
    }

    private Event eventOwnedBy(String owner) {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setOwnerUserId(owner);
        event.setTitle("Sortie partenaire");
        event.setStartAt(Instant.now().plusSeconds(3600));
        event.setEndAt(Instant.now().plusSeconds(7200));
        event.setVisibility(EventVisibility.PUBLIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCapacity(20);
        event.setRegisteredCount(0);
        return event;
    }
}
