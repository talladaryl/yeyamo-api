package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AdventureCriteriaResponse(
        String countryCode, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime,
        AdventurePartyType partyType, List<String> interestCodes, AdventureBudgetTier budgetTier,
        BigDecimal minimumAmount, BigDecimal maximumAmount, String currencyCode) {
}
