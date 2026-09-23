package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.util.List;

public record AdventurePlanPreviewResponse(
        AdventureCriteriaResponse normalizedCriteria,
        List<AdventureDayResponse> days,
        List<String> warnings,
        List<AdventureRelaxationResponse> relaxations,
        String noResultsReason) {
}
