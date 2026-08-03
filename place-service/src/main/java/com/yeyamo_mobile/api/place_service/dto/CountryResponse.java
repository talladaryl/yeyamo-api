package com.yeyamo_mobile.api.place_service.dto;

import com.yeyamo_mobile.api.place_service.enums.CountryLaunchStatus;
import com.yeyamo_mobile.api.place_service.models.Country;

public record CountryResponse(
        String countryCode,
        String name,
        CountryLaunchStatus launchStatus,
        String defaultCurrencyCode,
        String defaultTimezone) {
    public static CountryResponse from(Country country) {
        return new CountryResponse(country.getCode(), country.getName(), country.getLaunchStatus(),
                country.getDefaultCurrencyCode(), country.getDefaultTimezone());
    }
}
