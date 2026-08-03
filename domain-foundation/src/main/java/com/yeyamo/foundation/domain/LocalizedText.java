package com.yeyamo.foundation.domain;

public record LocalizedText(String languageCode, String title, String description) {
    public LocalizedText {
        languageCode = Standards.languageCode(languageCode);
        title = Standards.required(title, "title");
        description = Standards.optional(description);
    }
}
