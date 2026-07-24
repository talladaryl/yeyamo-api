package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;

public record SocialSettingsResponse(
        PrivacySettingsResponse privacy,
        SocialNotificationSettingsResponse notifications,
        SocialPreferenceSettingsResponse preferences) {

    public static SocialSettingsResponse from(UserProfile profile) {
        return new SocialSettingsResponse(
                new PrivacySettingsResponse(profile.getVisibility(), profile.isShowActivity(),
                        profile.isShowFollowers(), profile.isShowFollowing()),
                new SocialNotificationSettingsResponse(profile.isNotifyNewFollowers(),
                        profile.isNotifyFollowRequests(), profile.isNotifyMentions(),
                        profile.isNotifyActivityUpdates()),
                new SocialPreferenceSettingsResponse(profile.isAllowSuggestions(),
                        profile.isAllowMessagesFromStrangers()));
    }

    public record PrivacySettingsResponse(
            ProfileVisibility profileVisibility,
            boolean showActivity,
            boolean showFollowers,
            boolean showFollowing) {
    }

    public record SocialNotificationSettingsResponse(
            boolean newFollowers,
            boolean followRequests,
            boolean mentions,
            boolean activityUpdates) {
    }

    public record SocialPreferenceSettingsResponse(
            boolean allowSuggestions,
            boolean allowMessagesFromStrangers) {
    }
}
