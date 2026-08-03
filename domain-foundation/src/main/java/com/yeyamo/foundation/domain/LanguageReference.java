package com.yeyamo.foundation.domain;

public record LanguageReference(String languageCode) {
    public LanguageReference {
        languageCode = Standards.languageCode(languageCode);
    }
}
