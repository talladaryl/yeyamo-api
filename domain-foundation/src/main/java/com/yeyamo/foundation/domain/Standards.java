package com.yeyamo.foundation.domain;

import java.util.Currency;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Set;

final class Standards {
    private static final Set<String> COUNTRY_CODES = Set.of(Locale.getISOCountries());

    private Standards() {
    }

    static String countryCode(String value) {
        String code = required(value, "countryCode").toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODES.contains(code)) {
            throw new IllegalArgumentException("countryCode must be an ISO 3166-1 alpha-2 code");
        }
        return code;
    }

    static String languageCode(String value) {
        String code = required(value, "languageCode");
        try {
            new Locale.Builder().setLanguageTag(code);
        } catch (IllformedLocaleException exception) {
            throw new IllegalArgumentException("languageCode must be a valid BCP 47 tag", exception);
        }
        Locale locale = Locale.forLanguageTag(code);
        if (locale.getLanguage().isBlank()) {
            throw new IllegalArgumentException("languageCode must include a language subtag");
        }
        return locale.toLanguageTag();
    }

    static String currencyCode(String value) {
        String code = required(value, "currencyCode").toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(code).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("currencyCode must be an ISO 4217 code", exception);
        }
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
