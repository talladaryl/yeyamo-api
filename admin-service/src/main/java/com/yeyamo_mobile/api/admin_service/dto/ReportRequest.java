package com.yeyamo_mobile.api.admin_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.ReportType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull ReportType reportType,
        @NotNull UUID targetId,
        @NotNull UUID reporterId,
        @NotBlank @Size(max = 255) String reason,
        String description
) {
}
