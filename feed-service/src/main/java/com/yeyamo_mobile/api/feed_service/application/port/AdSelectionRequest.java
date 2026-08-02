package com.yeyamo_mobile.api.feed_service.application.port;

import java.util.List;

/**
 * Request for ad selection from ads-delivery-service
 */
public record AdSelectionRequest(
    String userId,
    String placementType,
    int maxAds,
    String contextType,
    List<String> excludedCampaigns,
    String correlationId
) {}
