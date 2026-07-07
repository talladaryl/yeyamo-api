package com.yeyamo_mobile.api.admin_service.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;

import jakarta.validation.constraints.NotNull;

public record PlaceReviewRequest(
        @NotNull ValidationStatus status,
        UUID reviewedBy,
        String reviewComment,
        Map<String, Object> changesRequested
) {
    public Map<String, Object> safeChangesRequested() {
        return changesRequested == null ? new LinkedHashMap<>() : changesRequested;
    }
}
