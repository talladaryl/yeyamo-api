package com.yeyamo_mobile.api.country_config_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCountryRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 200) String officialName,
    @NotBlank @Size(max = 10) String defaultLanguageCode,
    @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String defaultCurrencyCode,
    @NotBlank @Size(max = 50) String defaultTimezone,
    @NotBlank @Size(max = 5) @Pattern(regexp = "\\+\\d{1,4}") String phoneCountryCode
) {
}
