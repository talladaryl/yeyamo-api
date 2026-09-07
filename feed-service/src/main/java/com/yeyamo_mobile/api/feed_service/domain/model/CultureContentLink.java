package com.yeyamo_mobile.api.feed_service.domain.model;

import java.util.UUID;

/** Local read model of the cultural targets that may be displayed by the feed. */
public record CultureContentLink(UUID contentId, String type, String title, boolean active) {
    public CultureContentLink {
        if (contentId == null) throw new IllegalArgumentException("contentId is required");
        if (!"PROVERB".equals(type) && !"RECIPE".equals(type)) {
            throw new IllegalArgumentException("Unsupported cultural target type: " + type);
        }
        title = title == null || title.isBlank() ? null : title.trim();
    }
}
