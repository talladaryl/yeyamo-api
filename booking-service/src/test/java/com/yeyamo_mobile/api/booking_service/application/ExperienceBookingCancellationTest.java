package com.yeyamo_mobile.api.booking_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.booking_service.application.port.BookingOutboxPort;
import com.yeyamo_mobile.api.booking_service.domain.ActivityType;
import com.yeyamo_mobile.api.booking_service.domain.PaymentStatus;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.ActivitySlotEntity;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.ActivitySlotRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.BookingEntity;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.BookingHistoryRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.BookingRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.CommandReceiptRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.PaymentSagaRepository;

class ExperienceBookingCancellationTest {
    private final ActivitySlotRepository slots = mock(ActivitySlotRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PaymentSagaRepository sagas = mock(PaymentSagaRepository.class);
    private final CommandReceiptRepository receipts = mock(CommandReceiptRepository.class);
    private final BookingHistoryRepository history = mock(BookingHistoryRepository.class);
    private final BookingOutboxPort outbox = mock(BookingOutboxPort.class);
    private final BookingReferenceGenerator references = mock(BookingReferenceGenerator.class);
    private BookingApplicationService service;

    @BeforeEach
    void setUp() {
        service = new BookingApplicationService(slots, bookings, sagas, receipts, history, outbox, references);
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(slots.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void paidExperienceCancellationDoesNotRequestAnAutomaticRefund() {
        ActivitySlotEntity slot = ActivitySlotEntity.create("experience-1", ActivityType.EXPERIENCE, "partner-1",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 5, true,
                new BigDecimal("10000.00"), "XAF", "CM", null);
        slot.reserve(1);
        BookingEntity booking = BookingEntity.pending("YY-EXP", "customer-1", slot, 1);
        booking.confirm();

        when(bookings.findLocked(booking.getId())).thenReturn(Optional.of(booking));
        when(slots.findLocked(slot.getId())).thenReturn(Optional.of(slot));

        var cancelled = service.cancel("customer-1", booking.getId(), "indisponible", "cancel-exp-1", "corr-1", false);

        assertEquals(PaymentStatus.REFUND_NOT_REQUESTED, cancelled.paymentStatus());
        verify(outbox, never()).append(eq("payment.commands"), eq("payment.refund.requested"), anyString(), anyString(), anyMap());
        verify(outbox, never()).append(eq("payment.commands"), eq("payment.authorization.cancel.requested"), anyString(), anyString(), anyMap());
    }
}
