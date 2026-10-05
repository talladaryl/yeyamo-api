package com.yeyamo_mobile.api.event_service.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.RegistrationStatus;
import com.yeyamo_mobile.api.event_service.repository.EventRegistrationRepository;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;

/** Internal-only proof source for verified event reviews. */
@RestController
@RequestMapping("/internal/review-eligibility")
public class InternalEventReviewEligibilityController {
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final String internalToken;

    public InternalEventReviewEligibilityController(EventRepository events, EventRegistrationRepository registrations,
            @Value("${yeyamo.security.internal-token:${INTERNAL_SERVICE_TOKEN:}}") String internalToken) {
        this.events = events;
        this.registrations = registrations;
        this.internalToken = internalToken;
    }

    @GetMapping("/events/{eventId}/users/{userId}")
    public Eligibility event(@PathVariable UUID eventId, @PathVariable String userId, @RequestHeader("X-Internal-Token") String token) {
        requireToken(token);
        var event = events.findById(eventId);
        if (event.isEmpty()) return new Eligibility(false, null, "TARGET_NOT_FOUND");
        boolean completed = event.get().getStatus() == EventStatus.COMPLETED;
        if (!completed) return new Eligibility(false, null, "EVENT_NOT_COMPLETED");
        boolean registered = registrations.existsByEventIdAndUserIdAndStatus(eventId, userId, RegistrationStatus.CONFIRMED);
        if (!registered) return new Eligibility(false, null, "REGISTRATION_NOT_CONFIRMED");
        return new Eligibility(true, eventId.toString(), "ELIGIBLE");
    }

    private void requireToken(String token) {
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal token required");
        }
    }
    public record Eligibility(boolean eligible, String transactionId, String reasonCode) { }
}
