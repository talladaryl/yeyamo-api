package com.yeyamo_mobile.api.discovery_service.application;

import com.yeyamo_mobile.api.discovery_service.application.port.*;
import com.yeyamo_mobile.api.discovery_service.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdInjectionService {

    private static final Logger logger = LoggerFactory.getLogger(AdInjectionService.class);
    
    private final AdsDeliveryPort adsDeliveryPort;
    private final boolean discoveryAdsEnabled;
    private final int maxAdsPerPage;
    private final int organicPerAd;
    
    public AdInjectionService(
            AdsDeliveryPort adsDeliveryPort,
            @Value("${yeyamo.ads.discovery.enabled:false}") boolean discoveryAdsEnabled,
            @Value("${yeyamo.ads.discovery.max-per-page:3}") int maxAdsPerPage,
            @Value("${yeyamo.ads.discovery.organic-per-ad:8}") int organicPerAd) {
        
        this.adsDeliveryPort = adsDeliveryPort;
        this.discoveryAdsEnabled = discoveryAdsEnabled;
        this.maxAdsPerPage = maxAdsPerPage;
        this.organicPerAd = organicPerAd;
        
        logger.info("AdInjectionService initialized: enabled={}, maxPerPage={}, organicPerAd={}", 
            discoveryAdsEnabled, maxAdsPerPage, organicPerAd);
    }

    public List<DiscoveryItem> injectAds(
            List<DiscoveryDocument> organicDocs, 
            String userId, 
            int page,
            String placementContext,
            String correlationId) {
        
        if (!discoveryAdsEnabled) {
            logger.debug("Discovery ads disabled by feature flag");
            return organicDocs.stream().map(DiscoveryItem::fromDocument).toList();
        }
        
        if (organicDocs == null || organicDocs.isEmpty()) {
            logger.debug("No organic items to inject ads into");
            return organicDocs != null ? 
                organicDocs.stream().map(DiscoveryItem::fromDocument).toList() : List.of();
        }
        
        if (!adsDeliveryPort.isHealthy()) {
            logger.warn("Ads delivery service unhealthy - returning organic results only");
            return organicDocs.stream().map(DiscoveryItem::fromDocument).toList();
        }
        
        int maxAdsForContent = organicDocs.size() / organicPerAd;
        int adsToRequest = Math.min(maxAdsForContent, maxAdsPerPage);
        
        if (adsToRequest <= 0) {
            logger.debug("Not enough organic content for ad injection");
            return organicDocs.stream().map(DiscoveryItem::fromDocument).toList();
        }
        
        var request = new AdSelectionRequest(
            userId,
            "DISCOVERY",
            adsToRequest,
            placementContext,
            List.of(),
            correlationId
        );
        
        Optional<List<SponsoredPlacement>> placementsOpt = adsDeliveryPort.requestPlacements(request);
        
        if (placementsOpt.isEmpty() || placementsOpt.get().isEmpty()) {
            logger.info("No sponsored placements available - returning organic results only");
            return organicDocs.stream().map(DiscoveryItem::fromDocument).toList();
        }
        
        List<SponsoredPlacement> placements = placementsOpt.get();
        logger.info("Injecting {} sponsored placements into {} organic items", placements.size(), organicDocs.size());
        
        return injectAtPositions(organicDocs, placements);
    }
    
    private List<DiscoveryItem> injectAtPositions(List<DiscoveryDocument> organicDocs, List<SponsoredPlacement> placements) {
        List<DiscoveryItem> result = new ArrayList<>(organicDocs.size() + placements.size());
        
        int organicIndex = 0;
        int adIndex = 0;
        int itemsSinceLastAd = 0;
        
        while (organicIndex < organicDocs.size()) {
            result.add(DiscoveryItem.fromDocument(organicDocs.get(organicIndex)));
            organicIndex++;
            itemsSinceLastAd++;
            
            boolean shouldInjectAd = adIndex < placements.size() 
                && itemsSinceLastAd >= organicPerAd;
            
            if (shouldInjectAd) {
                SponsoredPlacement placement = placements.get(adIndex);
                result.add(DiscoveryItem.sponsored(
                    placement.deliveryId(),
                    placement.campaignId(),
                    placement.promotedEntityType(),
                    placement.promotedEntityId(),
                    placement.creative(),
                    placement.bidAmount(),
                    placement.trackingToken()
                ));
                adIndex++;
                itemsSinceLastAd = 0;
                
                logger.debug("Injected ad at position {} (campaignId={})", result.size() - 1, placement.campaignId());
            }
        }
        
        logger.info("Discovery injection complete: {} organic + {} sponsored = {} total items",
            organicDocs.size(), adIndex, result.size());
        
        return result;
    }
}
