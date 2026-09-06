package com.yeyamo_mobile.api.booking_service.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.booking_service.application.BookingDtos.*;
import com.yeyamo_mobile.api.booking_service.application.port.BookingOutboxPort;
import com.yeyamo_mobile.api.booking_service.domain.BookingException;
import com.yeyamo_mobile.api.booking_service.infrastructure.messaging.PlaceEventConsumer;
import com.yeyamo_mobile.api.booking_service.infrastructure.messaging.ProcessedEventEntity;
import com.yeyamo_mobile.api.booking_service.infrastructure.messaging.ProcessedEventRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.*;

class PlaceActivityIntegrationTest {

    ActivitySlotRepository slots = mock(ActivitySlotRepository.class);
    BookingRepository bookings = mock(BookingRepository.class);
    PaymentSagaRepository sagas = mock(PaymentSagaRepository.class);
    CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
    BookingHistoryRepository history = mock(BookingHistoryRepository.class);
    BookingOutboxPort outbox = mock(BookingOutboxPort.class);
    BookingReferenceGenerator references = mock(BookingReferenceGenerator.class);
    PlaceReadModelRepository placeRepo = mock(PlaceReadModelRepository.class);
    ProcessedEventRepository processedRepo = mock(ProcessedEventRepository.class);

    BookingApplicationService service;
    PlaceEventConsumer consumer;

    @BeforeEach
    void setUp() {
        service = new BookingApplicationService(
                slots, bookings, sagas, receipts, history, outbox, references, null, placeRepo
        );
        consumer = new PlaceEventConsumer(new ObjectMapper(), placeRepo, processedRepo);
    }

    @Test
    void createSlotWithValidActivePlaceSucceeds() {
        UUID placeId = UUID.randomUUID();
        when(placeRepo.findByPlaceIdAndActiveTrue(placeId))
                .thenReturn(Optional.of(new PlaceReadModelEntity(placeId, "Musée des Civilisations", true, Instant.now())));

        when(slots.save(any(ActivitySlotEntity.class))).thenAnswer(i -> i.getArgument(0));

        CreateSlot command = new CreateSlot(
                "visite-guidee",
                placeId,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                20,
                new BigDecimal("15.00"),
                "XOF",
                "CI"
        );

        SlotView view = service.createSlot(command, "partner-1", "corr-1");
        assertNotNull(view);
        assertEquals("visite-guidee", view.activityId());
        assertEquals(placeId, view.placeId());
        assertEquals("CI", view.countryCode());
        verify(slots).save(any(ActivitySlotEntity.class));
    }

    @Test
    void createSlotWithUnknownPlaceThrowsInvalidPlace() {
        UUID placeId = UUID.randomUUID();
        when(placeRepo.findByPlaceIdAndActiveTrue(placeId)).thenReturn(Optional.empty());

        CreateSlot command = new CreateSlot(
                "visite-guidee",
                placeId,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                20,
                new BigDecimal("15.00"),
                "XOF",
                "CI"
        );

        BookingException ex = assertThrows(BookingException.class, () -> service.createSlot(command, "partner-1", "corr-1"));
        assertEquals("INVALID_PLACE", ex.code());
        verify(slots, never()).save(any());
    }

    @Test
    void listActivitiesByPlaceReturnsPaginatedResults() {
        UUID placeId = UUID.randomUUID();
        ActivitySlotEntity slot1 = ActivitySlotEntity.create("act-1", "partner-1", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10, BigDecimal.TEN, "EUR", "CI", placeId);
        Pageable pageable = PageRequest.of(0, 20);

        when(slots.findByPlaceIdAndStartsAtAfterOrderByStartsAtAsc(eq(placeId), any(Instant.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(slot1), pageable, 1));

        Page<SlotView> page = service.listActivities(placeId, pageable);
        assertEquals(1, page.getTotalElements());
        assertEquals("act-1", page.getContent().get(0).activityId());
        assertEquals(placeId, page.getContent().get(0).placeId());
    }

    @Test
    void placeEventConsumerSynchronizesActivePlace() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        when(processedRepo.existsById(eventId)).thenReturn(false);
        when(placeRepo.findById(placeId)).thenReturn(Optional.empty());

        String raw = """
        {
            "eventId": "%s",
            "eventType": "place.created",
            "eventVersion": 1,
            "payload": {
                "placeId": "%s",
                "name": "Parc National de Taï",
                "status": "PUBLISHED"
            }
        }
        """.formatted(eventId, placeId);

        consumer.consume(raw);

        verify(placeRepo).save(argThat(p -> p.getPlaceId().equals(placeId) && p.isActive() && "Parc National de Taï".equals(p.getName())));
        verify(processedRepo).save(any(ProcessedEventEntity.class));
    }

    @Test
    void placeEventConsumerDeactivatesArchivedPlace() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID placeId = UUID.randomUUID();
        when(processedRepo.existsById(eventId)).thenReturn(false);
        PlaceReadModelEntity existing = new PlaceReadModelEntity(placeId, "Ancien Lieu", true, Instant.now());
        when(placeRepo.findById(placeId)).thenReturn(Optional.of(existing));

        String raw = """
        {
            "eventId": "%s",
            "eventType": "place.updated",
            "eventVersion": 1,
            "payload": {
                "placeId": "%s",
                "name": "Ancien Lieu",
                "status": "ARCHIVED"
            }
        }
        """.formatted(eventId, placeId);

        consumer.consume(raw);

        verify(placeRepo).save(argThat(p -> p.getPlaceId().equals(placeId) && !p.isActive()));
        verify(processedRepo).save(any(ProcessedEventEntity.class));
    }
}
