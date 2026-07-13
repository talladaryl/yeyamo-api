package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import com.yeyamo_mobile.api.user_service.domain.model.Language;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 2048) String avatarUrl,
        @Size(max = 500) String bio,
        Language language,
        ProfileVisibility visibility) {
}
