package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.time.LocalDate;
import java.util.List;

public record AdventureDayResponse(LocalDate date, int position, List<AdventureRecommendationResponse> items) {
}
