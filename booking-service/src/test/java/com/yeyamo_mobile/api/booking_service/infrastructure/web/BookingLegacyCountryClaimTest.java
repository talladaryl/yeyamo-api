package com.yeyamo_mobile.api.booking_service.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.yeyamo_mobile.api.booking_service.application.BookingApplicationService;
import com.yeyamo_mobile.api.booking_service.application.BookingReferenceGenerator;
import com.yeyamo_mobile.api.booking_service.application.BookingDtos.CreateBooking;
import com.yeyamo_mobile.api.booking_service.domain.BookingException;
import com.yeyamo_mobile.api.booking_service.application.port.BookingOutboxPort;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.ActivitySlotEntity;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.ActivitySlotRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.BookingHistoryRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.BookingRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.CommandReceiptRepository;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.PaymentSagaRepository;

class BookingLegacyCountryClaimTest {

    @Test
    void paidBookingWithLegacyJwtWithoutCountryReturnsTokenRefreshRequired() {
        ActivitySlotRepository slots = mock(ActivitySlotRepository.class);
        ActivitySlotEntity paidSlot = ActivitySlotEntity.create("activity-1", "partner-1",
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), 10,
                new BigDecimal("5000"), "XAF");
        when(slots.findLocked(paidSlot.getId())).thenReturn(Optional.of(paidSlot));
        BookingApplicationService service = new BookingApplicationService(
                slots, mock(BookingRepository.class), mock(PaymentSagaRepository.class),
                mock(CommandReceiptRepository.class), mock(BookingHistoryRepository.class),
                mock(BookingOutboxPort.class), mock(BookingReferenceGenerator.class));
        BookingController controller = new BookingController(service);
        Jwt legacyJwt = new Jwt("legacy-token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", "user-1"));

        BookingException exception = assertThrows(BookingException.class, () -> controller.create(
                new JwtAuthenticationToken(legacyJwt),
                new CreateBooking(paidSlot.getId(), 1, "mtn", "+237690123456"),
                "legacy-country-claim", null));

        ApiExceptionHandler.Problem problem = new ApiExceptionHandler().booking(exception).getBody();
        assertEquals("TOKEN_REFRESH_REQUIRED", problem.code());
        assertEquals(401, new ApiExceptionHandler().booking(exception).getStatusCode().value());
    }
}
