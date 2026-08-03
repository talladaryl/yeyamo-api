package com.yeyamo_mobile.api.place_service.dto;

import com.yeyamo_mobile.api.place_service.models.CountryLanguage;

public record CountryLanguageResponse(
        String languageCode,
        String displayName,
        boolean official,
        boolean primary) {
    public static CountryLanguageResponse from(CountryLanguage language) {
        return new CountryLanguageResponse(language.getLanguageCode(), language.getDisplayName(),
                language.isOfficial(), language.isPrimaryLanguage());
    }
}
