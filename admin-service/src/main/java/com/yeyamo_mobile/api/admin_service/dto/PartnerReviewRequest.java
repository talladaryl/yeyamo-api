package com.yeyamo_mobile.api.admin_service.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record PartnerReviewRequest(
        @NotNull @JsonAlias("status") ValidationStatus decision,
        @Size(max = 1000) String reason,
        @Size(max = 1000) String comment,
        Integer riskScore
) {
    public PartnerReviewRequest(ValidationStatus status, UUID ignoredValidatedBy, String reviewComment, Integer riskScore) {
        this(status, reviewComment, reviewComment, riskScore);
    }
}
