package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.util.Set;

import jakarta.validation.constraints.Size;

/**
 * Request to update user language preferences.
 */
public record UpdateLanguageRequest(
        @Size(max = 10) String preferredLanguageCode,
        Set<String> contentLanguages
) {
}
