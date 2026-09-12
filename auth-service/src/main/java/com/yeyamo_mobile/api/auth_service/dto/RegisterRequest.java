package com.yeyamo_mobile.api.auth_service.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to register a new user with multi-country support.
 * 
 * Geographic fields:
 * - countryCode: ISO 3166-1 alpha-2 (e.g., "CM", "SN")
 * - cityId: nullable, references country-config-service City
 * - preferredLanguageCode: ISO 639-1 or BCP 47 (e.g., "fr", "en")
 * - timezone: nullable, IANA timezone (e.g., "Africa/Douala")
 * 
 * Phone format: E.164 format required (+237XXXXXXXXX)
 */
public record RegisterRequest(
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "\\+[1-9]\\d{1,14}", message = "Phone must be in E.164 format") String phone,
        @Size(min = 12, max = 128) String password,
        @Size(max = 80) @Pattern(regexp = "[\\p{L}\\p{N} .'-]*") String displayName,
        @Size(max = 4096) String turnstileToken,
        @Pattern(regexp = "[A-Z]{2}", message = "Country code must be ISO 3166-1 alpha-2") String countryCode,
        UUID cityId,
        @Size(max = 10) String preferredLanguageCode,
        @Size(max = 50) String timezone
) {
}
