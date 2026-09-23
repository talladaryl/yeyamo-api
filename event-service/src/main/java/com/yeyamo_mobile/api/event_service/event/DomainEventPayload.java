package com.yeyamo_mobile.api.event_service.event;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.EventVisibility;

public record DomainEventPayload(
        UUID eventId,
        UUID placeId,
        String organizerUserId,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        EventStatus status,
        EventVisibility visibility,
        UUID coverMediaId,
        boolean publishToFeed,
        boolean publishToStory,
        String countryCode,
        String languageCode,
        String locationName,
        Double latitude,
        Double longitude
) {
}
