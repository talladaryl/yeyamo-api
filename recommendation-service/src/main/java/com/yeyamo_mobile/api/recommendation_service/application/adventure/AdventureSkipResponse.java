package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.util.UUID;

public record AdventureSkipResponse(UUID skippedRecommendationId, AdventureRecommendationResponse replacement, String reasonCode) {
}
