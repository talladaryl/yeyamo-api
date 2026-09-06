package com.yeyamo_mobile.api.payment_service.infrastructure.aggregator;

import java.math.BigDecimal;

public record CashInRequest(
    String operator,
    String country,
    String phoneNumber,
    BigDecimal amount,
    String currency,
    String idempotencyKey
) {
    public void validateContract() {
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("CONTRACT_GAP_MISSING_FIELD: operator is required by HR-Skills Pay but missing from producer command");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("CONTRACT_GAP_MISSING_FIELD: country is required by HR-Skills Pay but missing from producer command");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("CONTRACT_GAP_MISSING_FIELD: phone_number is required by HR-Skills Pay but missing from producer command");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be strictly positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
    }
}
