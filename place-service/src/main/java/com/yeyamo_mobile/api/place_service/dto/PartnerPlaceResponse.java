package com.yeyamo_mobile.api.place_service.dto;

import java.util.UUID;

/**
 * Minimal, partner-owned place representation used by the event creation selector.
 */
public record PartnerPlaceResponse(UUID id, String name, String status) {

    public static PartnerPlaceResponse from(AdminPlaceResponse place) {
        return new PartnerPlaceResponse(place.id(), place.name(), place.status());
    }
}
