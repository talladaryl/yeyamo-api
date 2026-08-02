package com.yeyamo_mobile.api.ads_delivery_service.domain.port;

import com.yeyamo_mobile.api.ads_delivery_service.application.dto.AdSelectionRequest;
import com.yeyamo_mobile.api.ads_delivery_service.application.dto.SponsoredPlacementResponse;

import java.util.List;

/**
 * Interface for integration with other services (feed, discovery, search)
 * This allows additive integration without modifying organic ranking
 */
public interface SponsoredPlacementProvider {
    
    /**
     * Get sponsored placements for a given context
     * Returns empty list if ads are disabled or no eligible ads found
     */
    List<SponsoredPlacementResponse> getSponsoredPlacements(AdSelectionRequest request);
}
