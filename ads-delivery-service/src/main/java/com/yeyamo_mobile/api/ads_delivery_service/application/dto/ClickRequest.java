package com.yeyamo_mobile.api.ads_delivery_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ClickRequest(
    @NotBlank String clickToken,
    @NotNull Instant clickedAt
) {
}
