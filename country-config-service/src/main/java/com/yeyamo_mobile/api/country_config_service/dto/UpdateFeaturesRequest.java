package com.yeyamo_mobile.api.country_config_service.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateFeaturesRequest(
    @NotNull Boolean registrationEnabled,
    @NotNull Boolean contentPublishingEnabled,
    @NotNull Boolean partnerOnboardingEnabled,
    @NotNull Boolean paymentsEnabled,
    @NotNull Boolean bookingEnabled,
    @NotNull Boolean ticketingEnabled,
    @NotNull Boolean artisanCommerceEnabled,
    @NotNull Boolean cultureModuleEnabled
) {
}
