package com.yeyamo_mobile.api.content_service.interfaces.rest;

import jakarta.validation.constraints.Pattern;

/** Structured, non-executable Story caption styling. */
public record StoryCaptionStyleRequest(
        @Pattern(regexp = "SYSTEM|SERIF|MONOSPACE|SANS_SERIF|CONDENSED", message = "Unsupported Story caption font")
        String fontFamily,
        Boolean bold,
        Boolean italic,
        Boolean underline,
        Boolean strikethrough
) {
    public static final String DEFAULT_FONT = "SYSTEM";

    public String normalizedFontFamily() {
        return fontFamily == null || fontFamily.isBlank() ? DEFAULT_FONT : fontFamily;
    }

    public boolean isBold() { return Boolean.TRUE.equals(bold); }
    public boolean isItalic() { return Boolean.TRUE.equals(italic); }
    public boolean isUnderline() { return Boolean.TRUE.equals(underline); }
    public boolean isStrikethrough() { return Boolean.TRUE.equals(strikethrough); }
}
