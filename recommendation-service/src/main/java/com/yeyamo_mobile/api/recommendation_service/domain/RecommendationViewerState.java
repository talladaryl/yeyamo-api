package com.yeyamo_mobile.api.recommendation_service.domain;

/** Only persisted viewer-specific states available in this projection are exposed. */
public record RecommendationViewerState(String feedbackType) {
    public static final RecommendationViewerState NONE = new RecommendationViewerState(null);
}
