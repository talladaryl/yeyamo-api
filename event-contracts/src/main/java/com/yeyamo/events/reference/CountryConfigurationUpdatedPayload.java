package com.yeyamo.events.reference;

public record CountryConfigurationUpdatedPayload(
        String countryCode,
        String launchStatus,
        String defaultCurrencyCode,
        String defaultTimezone) {
}
