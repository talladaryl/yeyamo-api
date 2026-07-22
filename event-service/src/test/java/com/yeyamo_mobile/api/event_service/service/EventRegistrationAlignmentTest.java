package com.yeyamo_mobile.api.event_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.RegistrationStatus;
import com.yeyamo_mobile.api.event_service.event.EventPublisher;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.models.EventRegistration;
import com.yeyamo_mobile.api.event_service.repository.EventRegistrationRepository;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;

class EventRegistrationAlignmentTest {
    private EventRepository events;
    private EventRegistrationRepository registrations;
    private EventService service;

    @BeforeEach
    void setUp() {
        events = mock(EventRepository.class);
        registrations = mock(EventRegistrationRepository.class);
        service = new EventService(events, registrations, mock(EventPublisher.class));
    }

    @Test
    void registersWithTheStringJwtSubject() {
        Event event = publishedEvent();
        when(events.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(registrations.findByEventIdAndUserId(event.getId(), "42")).thenReturn(Optional.empty());
        when(registrations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(events.save(event)).thenReturn(event);

        service.register(event.getId(), "42");

        ArgumentCaptor<EventRegistration> captor = ArgumentCaptor.forClass(EventRegistration.class);
        verify(registrations).save(captor.capture());
        assertEquals("42", captor.getValue().getUserId());
        assertEquals(RegistrationStatus.CONFIRMED, captor.getValue().getStatus());
    }

    @Test
    void listsOnlyTheCurrentUsersConfirmedRegistrations() {
        Event event = publishedEvent();
        EventRegistration registration = new EventRegistration();
        registration.setEvent(event);
        registration.setUserId("42");
        registration.setStatus(RegistrationStatus.CONFIRMED);
        when(registrations.findByUserIdAndStatusOrderByRegisteredAtDesc(
                eq("42"), eq(RegistrationStatus.CONFIRMED), any(Pageable.class)))
                .thenReturn(List.of(registration));

        var result = service.findRegisteredByUser("42", 50);

        assertEquals(List.of(event.getId()), result.stream().map(e -> e.id()).toList());
    }

    private Event publishedEvent() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setPlaceId(UUID.randomUUID());
        event.setTitle("Event");
        event.setStartAt(Instant.now().plusSeconds(3600));
        event.setEndAt(Instant.now().plusSeconds(7200));
        event.setStatus(EventStatus.PUBLISHED);
        event.setCapacity(10);
        event.setRegisteredCount(0);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
