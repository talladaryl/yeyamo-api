package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;

public record UserProfileSummaryResponse(
        UUID id,
        String displayName,
        String avatarUrl,
        String bio,
        boolean isFollowing,
        long followersCount,
        long followingCount) {
    
    public static UserProfileSummaryResponse from(UserProfile p, boolean isFollowing, long followersCount, long followingCount) {
        return new UserProfileSummaryResponse(
                p.getId(),
                p.getDisplayName(),
                p.getAvatarUrl(),
                p.getBio(),
                isFollowing,
                followersCount,
                followingCount);
    }

    public static UserProfileSummaryResponse fromBasic(UserProfile p) {
        return new UserProfileSummaryResponse(
                p.getId(),
                p.getDisplayName(),
                p.getAvatarUrl(),
                p.getBio(),
                false,
                0,
                0);
    }
}
