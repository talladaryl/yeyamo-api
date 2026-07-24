package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;

import jakarta.validation.Valid;

public record SocialSettingsRequest(
        @Valid PrivacySettingsRequest privacy,
        @Valid SocialNotificationSettingsRequest notifications,
        @Valid SocialPreferenceSettingsRequest preferences) {

    public record PrivacySettingsRequest(
            ProfileVisibility profileVisibility,
            Boolean showActivity,
            Boolean showFollowers,
            Boolean showFollowing) {
    }

    public record SocialNotificationSettingsRequest(
            Boolean newFollowers,
            Boolean followRequests,
            Boolean mentions,
            Boolean activityUpdates) {
    }

    public record SocialPreferenceSettingsRequest(
            Boolean allowSuggestions,
            Boolean allowMessagesFromStrangers) {
    }
}
