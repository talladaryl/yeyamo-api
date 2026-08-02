package com.yeyamo_mobile.api.feed_service.application;

import com.yeyamo_mobile.api.feed_service.application.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service responsible for injecting sponsored content into organic feed
 * following strict business rules for placement and frequency
 */
@Service
public class AdInjectionService {

    private static final Logger logger = LoggerFactory.getLogger(AdInjectionService.class);
    
    private final AdsDeliveryPort adsDeliveryPort;
    private final boolean feedAdsEnabled;
    private final int maxAdsPerPage;
    private final int organicPerAd;
    
    public AdInjectionService(
            AdsDeliveryPort adsDeliveryPort,
            @Value("${yeyamo.ads.feed.enabled:false}") boolean feedAdsEnabled,
            @Value("${yeyamo.ads.feed.max-per-page:3}") int maxAdsPerPage,
            @Value("${yeyamo.ads.feed.organic-per-ad:8}") int organicPerAd) {
        
        this.adsDeliveryPort = adsDeliveryPort;
        this.feedAdsEnabled = feedAdsEnabled;
        this.maxAdsPerPage = maxAdsPerPage;
        this.organicPerAd = organicPerAd;
        
        logger.info("AdInjectionService initialized: enabled={}, maxPerPage={}, organicPerAd={}", 
            feedAdsEnabled, maxAdsPerPage, organicPerAd);
    }

    /**
     * Inject sponsored content into organic feed items
     * 
     * @param organicItems the ranked organic content
     * @param userId the user requesting the feed
     * @param page the page number
     * @param correlationId request correlation ID
     * @return combined feed with sponsored content injected
     */
    public List<FeedItem> injectAds(
            List<FeedItem> organicItems, 
            String userId, 
            int page,
            String correlationId) {
        
        // Feature flag check
        if (!feedAdsEnabled) {
            logger.debug("Feed ads disabled by feature flag");
            return organicItems;
        }
        
        // No organic content = no injection
        if (organicItems == null || organicItems.isEmpty()) {
            logger.debug("No organic items to inject ads into");
            return organicItems != null ? organicItems : List.of();
        }
        
        // Check service health
        if (!adsDeliveryPort.isHealthy()) {
            logger.warn("Ads delivery service unhealthy - returning organic feed only");
            return organicItems;
        }
        
        // Calculate how many ads we can inject based on organic content size
        int maxAdsForContent = organicItems.size() / organicPerAd;
        int adsToRequest = Math.min(maxAdsForContent, maxAdsPerPage);
        
        if (adsToRequest <= 0) {
            logger.debug("Not enough organic content for ad injection (need {} organic items per ad)", organicPerAd);
            return organicItems;
        }
        
        // Request sponsored placements
        var request = new AdSelectionRequest(
            userId,
            "FEED",
            adsToRequest,
            "feed_page_" + page,
            List.of(), // TODO: track shown campaigns across pages
            correlationId
        );
        
        Optional<List<SponsoredPlacement>> placementsOpt = adsDeliveryPort.requestPlacements(request);
        
        if (placementsOpt.isEmpty() || placementsOpt.get().isEmpty()) {
            logger.info("No sponsored placements available - returning organic feed only");
            return organicItems;
        }
        
        List<SponsoredPlacement> placements = placementsOpt.get();
        logger.info("Injecting {} sponsored placements into {} organic items", placements.size(), organicItems.size());
        
        // Inject ads at calculated positions
        return injectAtPositions(organicItems, placements);
    }
    
    /**
     * Inject sponsored placements at calculated positions ensuring:
     * - No two consecutive ads
     * - Evenly distributed
     * - Organic content is never hidden
     */
    private List<FeedItem> injectAtPositions(List<FeedItem> organicItems, List<SponsoredPlacement> placements) {
        List<FeedItem> result = new ArrayList<>(organicItems.size() + placements.size());
        
        int organicIndex = 0;
        int adIndex = 0;
        int itemsSinceLastAd = 0;
        
        while (organicIndex < organicItems.size()) {
            // Add organic item
            result.add(organicItems.get(organicIndex));
            organicIndex++;
            itemsSinceLastAd++;
            
            // Check if we should inject an ad
            boolean shouldInjectAd = adIndex < placements.size() 
                && itemsSinceLastAd >= organicPerAd;
            
            if (shouldInjectAd) {
                SponsoredPlacement placement = placements.get(adIndex);
                result.add(FeedItem.sponsored(
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
        
        logger.info("Feed injection complete: {} organic + {} sponsored = {} total items",
            organicItems.size(), adIndex, result.size());
        
        return result;
    }
}
