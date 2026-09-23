package com.yeyamo_mobile.api.place_service.event;

import java.time.Instant;
import java.util.UUID;

/** Versioned event envelope published through the existing place-service outbox. */
public record PlaceSuggestionEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String actorId,
        Payload payload
) {
    public static PlaceSuggestionEvent created(UUID suggestionId, String userId, String countryCode,
            String correlationId, String actorId) {
        return event("place.suggestion.created", suggestionId, userId, countryCode, null, null, correlationId, actorId);
    }

    public static PlaceSuggestionEvent approved(UUID suggestionId, String userId, UUID canonicalPlaceId,
            String countryCode, String name, String correlationId, String actorId) {
        return event("place.suggestion.approved", suggestionId, userId, countryCode, canonicalPlaceId, name,
                correlationId, actorId);
    }

    public static PlaceSuggestionEvent rejected(UUID suggestionId, String userId, String countryCode, String name,
            String correlationId, String actorId) {
        return event("place.suggestion.rejected", suggestionId, userId, countryCode, null, name, correlationId, actorId);
    }

    private static PlaceSuggestionEvent event(String type, UUID suggestionId, String userId, String countryCode,
            UUID canonicalPlaceId, String name, String correlationId, String actorId) {
        return new PlaceSuggestionEvent(UUID.randomUUID(), type, 1, Instant.now(), "place-service", "place-suggestion",
                suggestionId.toString(), correlationId, actorId,
                new Payload(suggestionId, userId, canonicalPlaceId, countryCode, name, Instant.now()));
    }

    public record Payload(UUID suggestionId, String userId, UUID canonicalPlaceId, String countryCode, String name,
            Instant decisionAt) { }
}
