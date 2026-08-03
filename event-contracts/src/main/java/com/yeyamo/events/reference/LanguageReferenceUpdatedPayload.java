package com.yeyamo.events.reference;

public record LanguageReferenceUpdatedPayload(
        String countryCode,
        String languageCode,
        String displayName,
        boolean official,
        boolean primary) {
}
