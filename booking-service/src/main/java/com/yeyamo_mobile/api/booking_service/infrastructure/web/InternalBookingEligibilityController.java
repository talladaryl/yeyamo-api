package com.yeyamo_mobile.api.booking_service.infrastructure.web;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.yeyamo_mobile.api.booking_service.domain.ActivityType;
import com.yeyamo_mobile.api.booking_service.domain.BookingStatus;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.BookingRepository;

/** Internal-only proof source for verified experience reviews. */
@RestController
@RequestMapping("/internal/review-eligibility")
public class InternalBookingEligibilityController {
    private final BookingRepository bookings;
    private final String internalToken;

    public InternalBookingEligibilityController(BookingRepository bookings,
            @Value("${yeyamo.security.internal-token:${INTERNAL_SERVICE_TOKEN:}}") String internalToken) {
        this.bookings = bookings;
        this.internalToken = internalToken;
    }

    @GetMapping("/experiences/{experienceId}/users/{userId}")
    public Eligibility experience(@PathVariable String experienceId, @PathVariable String userId,
            @RequestHeader("X-Internal-Token") String token) {
        requireToken(token);
        return bookings.findFirstByUserIdAndActivityIdAndActivityTypeAndStatusOrderByCompletedAtDesc(userId, experienceId,
                ActivityType.EXPERIENCE, BookingStatus.COMPLETED)
                .map(booking -> new Eligibility(true, booking.getId().toString()))
                .orElseGet(() -> new Eligibility(false, null));
    }

    private void requireToken(String token) {
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal token required");
        }
    }

    public record Eligibility(boolean eligible, String transactionId) { }
}
