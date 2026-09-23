package com.yeyamo_mobile.api.place_service.dto;

import java.util.List;
import java.util.UUID;

public record PlaceSuggestionDuplicateCheckResponse(List<Candidate> possibleDuplicates) {
    public record Candidate(String kind, UUID id, String name, String address, double distanceMeters,
            boolean certain) { }
}
