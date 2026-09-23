package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

public record AdventureBudgetRequest(
        AdventureBudgetTier tier,
        @DecimalMin("0.0") BigDecimal minimumAmount,
        @DecimalMin("0.0") BigDecimal maximumAmount,
        @Pattern(regexp = "[A-Za-z]{3}") String currencyCode) {
}
