package com.yeyamo_mobile.api.feed_service.application.port;

import java.util.List;
import java.util.Optional;

/**
 * Port for fetching sponsored content from ads-delivery-service
 */
public interface AdsDeliveryPort {
    
    /**
     * Request sponsored placements for feed injection
     * @param request the ad selection context
     * @return sponsored placements or empty if service unavailable
     */
    Optional<List<SponsoredPlacement>> requestPlacements(AdSelectionRequest request);
    
    /**
     * Check if ads delivery service is healthy
     * @return true if service is available
     */
    boolean isHealthy();
}
