package com.yeyamo_mobile.api.admin_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.ReportStatus;

import jakarta.validation.constraints.NotNull;

public record ReportResolutionRequest(
        @NotNull ReportStatus status,
        UUID assignedTo,
        UUID resolvedBy,
        String resolutionComment
) {
}
