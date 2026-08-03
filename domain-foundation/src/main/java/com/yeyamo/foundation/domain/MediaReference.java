package com.yeyamo.foundation.domain;

import java.util.Locale;
import java.util.UUID;

public record MediaReference(UUID mediaId, String mediaType, String altText, int displayOrder) {
    public MediaReference {
        if (mediaId == null) {
            throw new IllegalArgumentException("mediaId is required");
        }
        mediaType = Standards.required(mediaType, "mediaType").toUpperCase(Locale.ROOT);
        altText = Standards.optional(altText);
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder cannot be negative");
        }
    }
}
