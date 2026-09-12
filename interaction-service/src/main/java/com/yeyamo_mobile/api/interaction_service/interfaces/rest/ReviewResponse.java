package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity;

public record ReviewResponse(
    UUID id,
    String userId,
    UUID placeId,
    String targetType,
    String evidenceReference,
    short rating,
    String comment,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
    public static ReviewResponse from(ReviewEntity review) {
        return new ReviewResponse(
            review.getId(),
            review.getUserId(),
            review.getPlaceId(),
            review.getTargetType(),
            review.getEvidenceReference(),
            review.getRating(),
            review.getComment(),
            review.getStatus(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
