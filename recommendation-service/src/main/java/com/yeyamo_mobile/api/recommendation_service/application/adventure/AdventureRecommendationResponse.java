package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdventureRecommendationResponse(
        UUID recommendationId, AdventureTargetType targetType, String targetId, Instant scheduledAt,
        List<String> reasonCodes, String title, UUID imageMediaId, String locationLabel,
        Instant startsAt, Instant endsAt, BigDecimal price, String currencyCode,
        AdventureAvailabilityStatus availabilityStatus) {
}
