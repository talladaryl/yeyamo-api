package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

public record UpdatePreferencesRequest(
        boolean notificationsEnabled,
        boolean locationSharingEnabled,
        Long preferredRegionId) {
}
