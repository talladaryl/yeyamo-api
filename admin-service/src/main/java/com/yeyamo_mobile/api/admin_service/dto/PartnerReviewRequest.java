package com.yeyamo_mobile.api.admin_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;

import jakarta.validation.constraints.NotNull;

public record PartnerReviewRequest(
        @NotNull ValidationStatus status,
        UUID validatedBy,
        String reviewComment,
        Integer riskScore
) {
}
