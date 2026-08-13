package com.yeyamo_mobile.api.country_config_service.dto;

import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLaunchStatus;

import java.time.Instant;
import java.util.UUID;

public record CountryDto(
    UUID id,
    String code,
    String name,
    String officialName,
    String continentCode,
    String defaultLanguageCode,
    String defaultCurrencyCode,
    String defaultTimezone,
    String phoneCountryCode,
    CountryLaunchStatus launchStatus,
    Boolean registrationEnabled,
    Boolean contentPublishingEnabled,
    Boolean partnerOnboardingEnabled,
    Boolean paymentsEnabled,
    Boolean bookingEnabled,
    Boolean ticketingEnabled,
    Boolean artisanCommerceEnabled,
    Boolean cultureModuleEnabled,
    Instant createdAt,
    Instant updatedAt
) {
}
