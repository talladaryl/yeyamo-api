package com.yeyamo_mobile.api.ads_delivery_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record ConversionRequest(
    @NotBlank String deliveryId,
    @NotNull Instant convertedAt,
    @NotBlank String conversionType,
    BigDecimal conversionValue
) {
}
