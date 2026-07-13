package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.Language;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileStatus;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;

public record MyProfileResponse(
        UUID id,
        String displayName,
        String avatarUrl,
        String bio,
        Language language,
        ProfileVisibility visibility,
        ProfileStatus status,
        boolean notificationsEnabled,
        boolean locationSharingEnabled,
        Long preferredRegionId,
        Instant createdAt,
        Instant updatedAt) {
    public static MyProfileResponse from(UserProfile p) {
        return new MyProfileResponse(p.getId(), p.getDisplayName(), p.getAvatarUrl(), p.getBio(), p.getLanguage(),
                p.getVisibility(), p.getStatus(), p.isNotificationsEnabled(), p.isLocationSharingEnabled(),
                p.getPreferredRegionId(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
