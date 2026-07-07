package com.yeyamo_mobile.api.admin_service.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PlaceValidationRequest(
        @NotNull UUID placeId,
        @NotNull UUID submittedBy,
        String reviewComment,
        Map<String, Object> changesRequested
) {
    public Map<String, Object> safeChangesRequested() {
        return changesRequested == null ? new LinkedHashMap<>() : changesRequested;
    }
}
