package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.Language;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;

public record PublicProfileResponse(
        UUID id,
        String displayName,
        String avatarUrl,
        String bio,
        Language language,
        ProfileVisibility visibility,
        Instant createdAt) {
    public static PublicProfileResponse from(UserProfile p) {
        return new PublicProfileResponse(p.getId(), p.getDisplayName(), p.getAvatarUrl(), p.getBio(),
                p.getLanguage(), p.getVisibility(), p.getCreatedAt());
    }
}
