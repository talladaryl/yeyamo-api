package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdventurePlanSummaryResponse(
        UUID id, String countryCode, LocalDate startDate, LocalDate endDate, AdventurePartyType partyType,
        AdventureBudgetTier budgetTier, BigDecimal minimumAmount, BigDecimal maximumAmount, String currencyCode,
        int itemCount, Instant createdAt) {
}
