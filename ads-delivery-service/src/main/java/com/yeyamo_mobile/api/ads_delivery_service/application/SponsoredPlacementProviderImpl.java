package com.yeyamo_mobile.api.ads_delivery_service.application;

import com.yeyamo_mobile.api.ads_delivery_service.application.dto.AdSelectionRequest;
import com.yeyamo_mobile.api.ads_delivery_service.application.dto.SponsoredPlacementResponse;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.SponsoredPlacementProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of SponsoredPlacementProvider for use by other services
 */
@Service
public class SponsoredPlacementProviderImpl implements SponsoredPlacementProvider {

    private static final Logger logger = LoggerFactory.getLogger(SponsoredPlacementProviderImpl.class);

    private final AdDeliveryService adDeliveryService;

    public SponsoredPlacementProviderImpl(AdDeliveryService adDeliveryService) {
        this.adDeliveryService = adDeliveryService;
    }

    @Override
    public List<SponsoredPlacementResponse> getSponsoredPlacements(AdSelectionRequest request) {
        try {
            return adDeliveryService.selectAds(request);
        } catch (Exception e) {
            logger.error("Error getting sponsored placements", e);
            // Never fail the calling service - return empty list
            return List.of();
        }
    }
}
