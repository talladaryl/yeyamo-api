package com.yeyamo_mobile.api.event_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.RegistrationStatus;
import com.yeyamo_mobile.api.event_service.models.EventRegistration;

public record EventParticipantResponse(
        UUID registrationId,
        String userId,
        RegistrationStatus status,
        Instant registeredAt
) {
    public static EventParticipantResponse from(EventRegistration registration) {
        return new EventParticipantResponse(
                registration.getId(),
                registration.getUserId(),
                registration.getStatus(),
                registration.getRegisteredAt()
        );
    }
}
