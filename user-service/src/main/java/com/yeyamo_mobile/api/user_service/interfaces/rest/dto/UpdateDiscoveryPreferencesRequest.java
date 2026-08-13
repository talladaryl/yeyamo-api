package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to update discovery preferences (content countries, local radius, African content).
 */
public record UpdateDiscoveryPreferencesRequest(
        Set<@Pattern(regexp = "[A-Z]{2}") String> contentCountries,
        @Min(1) @Max(500) Integer localRadiusKm,
        Boolean discoverAfricanContent,
        @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String preferredCurrencyCode
) {
}
