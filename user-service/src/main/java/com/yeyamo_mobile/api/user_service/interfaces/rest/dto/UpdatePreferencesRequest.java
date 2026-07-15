package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import jakarta.validation.constraints.Positive;

public record UpdatePreferencesRequest(
        boolean notificationsEnabled,
        boolean locationSharingEnabled,
        @Positive Long preferredRegionId) {
}
