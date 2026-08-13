package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;

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
        String countryCode,
        UUID adminLevel1Id,
        UUID adminLevel2Id,
        UUID cityId,
        UUID localityId,
        String preferredLanguageCode,
        String timezone,
        String preferredCurrencyCode,
        Set<String> contentCountries,
        Set<String> contentLanguages,
        Integer localRadiusKm,
        boolean discoverAfricanContent,
        Instant createdAt,
        Instant updatedAt) {
    public static MyProfileResponse from(UserProfile p) {
        return new MyProfileResponse(p.getId(), p.getDisplayName(), p.getAvatarUrl(), p.getBio(), p.getLanguage(),
                p.getVisibility(), p.getStatus(), p.isNotificationsEnabled(), p.isLocationSharingEnabled(),
                p.getPreferredRegionId(), p.getCountryCode(), p.getAdminLevel1Id(), p.getAdminLevel2Id(),
                p.getCityId(), p.getLocalityId(), p.getPreferredLanguageCode(), p.getTimezone(),
                p.getPreferredCurrencyCode(), Set.copyOf(p.getContentCountries()), Set.copyOf(p.getContentLanguages()),
                p.getLocalRadiusKm(), p.isDiscoverAfricanContent(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
