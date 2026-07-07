package com.yeyamo_mobile.api.admin_service.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.ModerationActionType;
import com.yeyamo_mobile.api.admin_service.enums.ModerationTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModerationActionRequest(
        @NotNull ModerationActionType actionType,
        @NotNull ModerationTargetType targetType,
        @NotNull UUID targetId,
        String previousStatus,
        @NotBlank String newStatus,
        String reason,
        Map<String, Object> details
) {
    public Map<String, Object> safeDetails() {
        return details == null ? new LinkedHashMap<>() : details;
    }
}
