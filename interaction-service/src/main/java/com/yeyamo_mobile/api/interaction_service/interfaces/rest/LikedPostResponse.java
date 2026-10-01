package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.interaction_service.domain.model.PostRelation;

/** Minimal persisted-like reference. Post content is resolved by content-service. */
public record LikedPostResponse(UUID postId, Instant likedAt) {
    public static LikedPostResponse from(PostRelation relation) {
        return new LikedPostResponse(relation.postId(), relation.createdAt());
    }
}
