package com.yeyamo_mobile.api.discovery_service.application.port;

import java.util.List;

public record AdSelectionRequest(
    String userId,
    String placementType,
    int maxAds,
    String contextType,
    List<String> excludedCampaigns,
    String correlationId
) {}
