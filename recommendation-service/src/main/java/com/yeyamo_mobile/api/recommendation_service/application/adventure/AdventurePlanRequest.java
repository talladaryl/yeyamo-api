package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdventurePlanRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        @NotNull AdventurePartyType partyType,
        @Size(max = 20) List<@Pattern(regexp = "[A-Za-z0-9_-]{1,100}") String> interestCodes,
        @Valid AdventureBudgetRequest budget) {
}
