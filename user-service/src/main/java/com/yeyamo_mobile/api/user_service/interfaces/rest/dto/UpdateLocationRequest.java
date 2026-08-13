package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.util.UUID;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to update user location.
 */
public record UpdateLocationRequest(
        @Pattern(regexp = "[A-Z]{2}", message = "Country code must be ISO 3166-1 alpha-2") String countryCode,
        UUID adminLevel1Id,
        UUID adminLevel2Id,
        UUID cityId,
        UUID localityId,
        @Size(max = 50) String timezone
) {
}
