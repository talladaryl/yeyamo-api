package com.yeyamo_mobile.api.feed_service.application;

import java.time.Instant;
import java.util.List;

public record PublicFeedPage(int page, int size, boolean hasNext, List<PublicFeedItem> items, Instant generatedAt) { }
