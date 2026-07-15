package com.yeyamo_mobile.api.analytics_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlacePopularitySummary(
        UUID placeId,
        BigDecimal popularityScore,
        int views,
        int likes,
        int comments,
        int shares,
        int checkIns,
        int bookings,
        int posts
) {
}
