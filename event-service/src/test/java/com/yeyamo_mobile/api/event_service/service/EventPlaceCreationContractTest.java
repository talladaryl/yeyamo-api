package com.yeyamo_mobile.api.event_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;

import com.yeyamo_mobile.api.event_service.dto.EventRequest;
import com.yeyamo_mobile.api.event_service.event.EventPublisher;
import com.yeyamo_mobile.api.event_service.exception.ApiException;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.models.PlaceReadModel;
import com.yeyamo_mobile.api.event_service.repository.EventRegistrationRepository;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;
import com.yeyamo_mobile.api.event_service.repository.PlaceReadModelRepository;

class EventPlaceCreationContractTest {
    @Test
    void createsEventForAnActivePlaceReadModel() {
        EventRepository events = mock(EventRepository.class);
        PlaceReadModelRepository places = mock(PlaceReadModelRepository.class);
        UUID placeId = UUID.randomUUID();
        when(places.findById(placeId)).thenReturn(Optional.of(new PlaceReadModel(placeId, "Musee", true, Instant.now())));
        when(events.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0); event.setId(UUID.randomUUID()); return event;
        });
        EventService service = service(events, places);

        var response = service.create(request(placeId), "corr-1", "partner-1");

        assertEquals(placeId, response.placeId());
    }

    @Test
    void returnsCoverMediaIdWhenEventIsCreatedWithOne() {
        EventRepository events = mock(EventRepository.class);
        PlaceReadModelRepository places = mock(PlaceReadModelRepository.class);
        UUID placeId = UUID.randomUUID();
        UUID coverMediaId = UUID.randomUUID();
        when(places.findById(placeId)).thenReturn(Optional.of(new PlaceReadModel(placeId, "Musee", true, Instant.now())));
        when(events.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0); event.setId(UUID.randomUUID()); return event;
        });
        EventRequest request = request(placeId);
        request.setCoverMediaId(coverMediaId);

        var response = service(events, places).create(request, "corr-cover", "partner-1");

        assertEquals(coverMediaId, response.coverMediaId());
    }

    @Test
    void returnsNullCoverMediaIdForLegacyEvent() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setPlaceId(UUID.randomUUID());
        event.setTitle("Evenement existant");
        event.setStartAt(Instant.now().plusSeconds(3600));
        event.setEndAt(Instant.now().plusSeconds(7200));
        event.setCapacity(25);
        event.setRegisteredCount(0);

        assertEquals(null, com.yeyamo_mobile.api.event_service.dto.EventResponse.from(event).coverMediaId());
    }

    @Test
    void rejectsAnUnknownPlace() {
        PlaceReadModelRepository places = mock(PlaceReadModelRepository.class);
        UUID placeId = UUID.randomUUID();
        when(places.findById(placeId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service(mock(EventRepository.class), places).create(request(placeId), null, "partner-1"));

        assertEquals("PLACE_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsAnInactivePlace() {
        PlaceReadModelRepository places = mock(PlaceReadModelRepository.class);
        UUID placeId = UUID.randomUUID();
        when(places.findById(placeId)).thenReturn(Optional.of(new PlaceReadModel(placeId, "Musee", false, Instant.now())));

        ApiException exception = assertThrows(ApiException.class, () -> service(mock(EventRepository.class), places).create(request(placeId), null, "partner-1"));

        assertEquals("PLACE_NOT_ACTIVE", exception.getCode());
    }

    @Test
    void requiresTheMandatoryMobileFields() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(new EventRequest());

        // A physical event may now use either a canonical place or a complete
        // free-form location; that cross-field rule is enforced by EventService.
        assertEquals(4, violations.size());
    }

    private EventService service(EventRepository events, PlaceReadModelRepository places) {
        return new EventService(events, mock(EventRegistrationRepository.class), mock(EventPublisher.class), null, places);
    }

    private EventRequest request(UUID placeId) {
        EventRequest request = new EventRequest();
        request.setPlaceId(placeId); request.setTitle("Visite guidee"); request.setStartAt(Instant.now().plusSeconds(3600));
        request.setEndAt(Instant.now().plusSeconds(7200)); request.setCapacity(25); return request;
    }
}
