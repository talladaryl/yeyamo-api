package com.yeyamo_mobile.api.ads_delivery_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record ImpressionRequest(
    @NotBlank String impressionToken,
    @NotNull Instant viewedAt,
    @Positive Long viewDurationMs
) {
}
