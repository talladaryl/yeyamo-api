package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;

/**
 * Public identity needed to render a social content author.  Content records
 * deliberately use the immutable auth subject while social actions use the
 * profile UUID, so both identifiers must be returned together.
 */
public record FeedAuthorIdentityResponse(
        UUID profileId,
        String authUserId,
        String displayName,
        String avatarUrl,
        boolean isFollowing) {

    public static FeedAuthorIdentityResponse from(UserProfile profile, boolean isFollowing) {
        return new FeedAuthorIdentityResponse(
                profile.getId(),
                profile.getAuthUserId(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                isFollowing);
    }
}
