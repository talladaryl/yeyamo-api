package com.yeyamo_mobile.api.feed_service.application;

import java.time.Instant;
import java.util.List;

/**
 * Cached organic page and API response metadata for the personalized feed.
 * Sponsored items are injected only after this page has been read from cache.
 */
public record FeedPage(
        String userId,
        int page,
        int size,
        boolean hasNext,
        List<FeedItem> items,
        Instant generatedAt) {
}
