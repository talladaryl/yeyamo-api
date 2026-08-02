package com.yeyamo_mobile.api.discovery_service.application;

import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryItem;
import java.time.Instant;
import java.util.List;

/**
 * Discovery page that can contain both organic and sponsored content
 */
public record DiscoveryPageWithAds(
    int page,
    int size,
    boolean hasNext,
    List<DiscoveryItem> items,
    Instant generatedAt
) {}
