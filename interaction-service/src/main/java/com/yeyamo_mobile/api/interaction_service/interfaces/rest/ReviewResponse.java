package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity;

public record ReviewResponse(
    UUID id,
    String userId,
    UUID placeId,
    short rating,
    String comment,
    Instant createdAt,
    Instant updatedAt
) {
    public static ReviewResponse from(ReviewEntity review) {
        return new ReviewResponse(
            review.getId(),
            review.getUserId(),
            review.getPlaceId(),
            review.getRating(),
            review.getComment(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
