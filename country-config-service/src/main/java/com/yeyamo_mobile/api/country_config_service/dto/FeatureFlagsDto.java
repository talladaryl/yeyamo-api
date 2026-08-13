package com.yeyamo_mobile.api.country_config_service.dto;

public record FeatureFlagsDto(
    Boolean registrationEnabled,
    Boolean contentPublishingEnabled,
    Boolean partnerOnboardingEnabled,
    Boolean paymentsEnabled,
    Boolean bookingEnabled,
    Boolean ticketingEnabled,
    Boolean artisanCommerceEnabled,
    Boolean cultureModuleEnabled
) {
}
