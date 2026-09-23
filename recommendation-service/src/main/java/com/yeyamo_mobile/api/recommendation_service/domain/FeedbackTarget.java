package com.yeyamo_mobile.api.recommendation_service.domain;

/** A user-scoped interaction target, independent from a candidate source id. */
public record FeedbackTarget(String targetType, String targetId) {
    public FeedbackTarget {
        if (targetType == null || targetType.isBlank() || targetId == null || targetId.isBlank())
            throw new IllegalArgumentException("feedback target is required");
    }
}
