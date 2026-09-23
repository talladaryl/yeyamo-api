package com.yeyamo_mobile.api.content_service.application;

import java.time.Instant;
import java.util.UUID;

/** Validated command derived from a real event-service `event.published` event. */
public record EventPublishedSocialCommand(
        UUID sourceEventId,
        UUID eventId,
        String organizerUserId,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        boolean publicEvent,
        UUID coverMediaId,
        boolean publishToFeed,
        boolean publishToStory,
        String countryCode,
        String languageCode,
        String correlationId) {

    public boolean hasRequestedTarget() {
        return publishToFeed || publishToStory;
    }

    public String socialCaption() {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        return title == null ? null : title.trim();
    }
}
