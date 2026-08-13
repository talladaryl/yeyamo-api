package com.yeyamo_mobile.api.country_config_service.mapper;

import com.yeyamo_mobile.api.country_config_service.domain.model.*;
import com.yeyamo_mobile.api.country_config_service.dto.*;
import org.springframework.stereotype.Component;

@Component
public class CountryMapper {

    public CountryDto toDto(Country country) {
        return new CountryDto(
                country.getId(),
                country.getCode(),
                country.getName(),
                country.getOfficialName(),
                country.getContinentCode(),
                country.getDefaultLanguageCode(),
                country.getDefaultCurrencyCode(),
                country.getDefaultTimezone(),
                country.getPhoneCountryCode(),
                country.getLaunchStatus(),
                country.getRegistrationEnabled(),
                country.getContentPublishingEnabled(),
                country.getPlacePublishingEnabled(),
                country.getEventFeatureEnabled(),
                country.getPartnerOnboardingEnabled(),
                country.getPaymentsEnabled(),
                country.getBookingEnabled(),
                country.getTicketingEnabled(),
                country.getArtisanCommerceEnabled(),
                country.getCultureModuleEnabled(),
                country.getCreatedAt(),
                country.getUpdatedAt()
        );
    }

    public LanguageDto toDto(CountryLanguage language) {
        return new LanguageDto(
                language.getId(),
                language.getLanguageCode(),
                language.getName(),
                language.getIsDefault(),
                language.getDisplayOrder()
        );
    }

    public CurrencyDto toDto(CountryCurrency currency) {
        return new CurrencyDto(
                currency.getId(),
                currency.getCurrencyCode(),
                currency.getName(),
                currency.getSymbol(),
                currency.getDecimalPlaces(),
                currency.getIsDefault()
        );
    }

    public TimezoneDto toDto(CountryTimezone timezone) {
        return new TimezoneDto(
                timezone.getId(),
                timezone.getTimezone(),
                timezone.getDisplayName(),
                timezone.getIsDefault()
        );
    }
}
