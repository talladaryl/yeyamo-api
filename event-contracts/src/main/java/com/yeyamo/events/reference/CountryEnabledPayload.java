package com.yeyamo.events.reference;

public record CountryEnabledPayload(
        String countryCode,
        String launchStatus,
        String defaultCurrencyCode,
        String defaultTimezone) {
}
