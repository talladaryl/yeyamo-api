package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdventurePlanDetailResponse(
        UUID id, AdventureCriteriaResponse criteria, List<AdventureDayResponse> days,
        Instant createdAt, Instant updatedAt) {
}
