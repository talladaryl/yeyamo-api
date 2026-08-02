package com.yeyamo_mobile.api.discovery_service.infrastructure.ads;

import com.yeyamo_mobile.api.discovery_service.application.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "yeyamo.ads.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAdsDeliveryClient implements AdsDeliveryPort {

    private static final Logger logger = LoggerFactory.getLogger(NoOpAdsDeliveryClient.class);
    
    public NoOpAdsDeliveryClient() {
        logger.info("Ads delivery DISABLED - using NoOp client");
    }

    @Override
    public Optional<List<SponsoredPlacement>> requestPlacements(AdSelectionRequest request) {
        return Optional.empty();
    }

    @Override
    public boolean isHealthy() {
        return false;
    }
}
