package com.yeyamo_mobile.api.country_config_service.dto;

import java.util.List;

public record CountryConfigurationDto(
    CountryDto country,
    List<LanguageDto> languages,
    List<CurrencyDto> currencies,
    List<TimezoneDto> timezones
) {
}
