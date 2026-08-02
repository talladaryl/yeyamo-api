package com.yeyamo_mobile.api.feed_service.application;

import com.yeyamo_mobile.api.feed_service.application.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdInjectionServiceTest {

    private AdsDeliveryPort adsDeliveryPort;
    private AdInjectionService adInjectionService;
    
    @BeforeEach
    void setUp() {
        adsDeliveryPort = mock(AdsDeliveryPort.class);
    }

    @Test
    @DisplayName("Feature flag disabled - returns organic feed only")
    void testFeatureFlagDisabled() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, false, 3, 8);
        
        List<FeedItem> organic = createOrganicItems(10);
        List<FeedItem> result = adInjectionService.injectAds(organic, "user1", 0, "corr-1");
        
        assertEquals(10, result.size());
        assertTrue(result.stream().allMatch(FeedItem::isOrganic));
        verify(adsDeliveryPort, never()).requestPlacements(any());
    }

    @Test
    @DisplayName("Ads service unhealthy - returns organic feed only")
    void testAdsServiceUnhealthy() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 3, 8);
        when(adsDeliveryPort.isHealthy()).thenReturn(false);
        
        List<FeedItem> organic = createOrganicItems(20);
        List<FeedItem> result = adInjectionService.injectAds(organic, "user1", 0, "corr-1");
        
        assertEquals(20, result.size());
        assertTrue(result.stream().allMatch(FeedItem::isOrganic));
        verify(adsDeliveryPort, never()).requestPlacements(any());
    }

    @Test
    @DisplayName("Not enough organic content - no ads injected")
    void testNotEnoughOrganicContent() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 3, 8);
        when(adsDeliveryPort.isHealthy()).thenReturn(true);
        
        List<FeedItem> organic = createOrganicItems(5); // Less than 8 required per ad
        List<FeedItem> result = adInjectionService.injectAds(organic, "user1", 0, "corr-1");
        
        assertEquals(5, result.size());
        assertTrue(result.stream().allMatch(FeedItem::isOrganic));
        verify(adsDeliveryPort, never()).requestPlacements(any());
    }

    @Test
    @DisplayName("Successful injection - 1 ad for 10 organic items")
    void testSuccessfulInjection() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 3, 8);
        when(adsDeliveryPort.isHealthy()).thenReturn(true);
        when(adsDeliveryPort.requestPlacements(any()))
            .thenReturn(Optional.of(createSponsoredPlacements(1)));
        
        List<FeedItem> organic = createOrganicItems(10);
        List<FeedItem> result = adInjectionService.injectAds(organic, "user1", 0, "corr-1");
        
        assertEquals(11, result.size());
        long adsCount = result.stream().filter(FeedItem::isSponsored).count();
        assertEquals(1, adsCount);
        
        // Ad should be injected after 8 organic items
        assertTrue(result.get(8).isSponsored());
    }

    @Test
    @DisplayName("Max ads limit respected")
    void testMaxAdsLimit() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 2, 8);
        when(adsDeliveryPort.isHealthy()).thenReturn(true);
        when(adsDeliveryPort.requestPlacements(any()))
            .thenReturn(Optional.of(createSponsoredPlacements(2)));
        
        List<FeedItem> organic = createOrganicItems(30); // Could fit 3 ads, but max is 2
        List<FeedItem> result = adInjectionService.injectAds(organic, "user1", 0, "corr-1");
        
        long adsCount = result.stream().filter(FeedItem::isSponsored).count();
        assertEquals(2, adsCount);
    }

    @Test
    @DisplayName("No consecutive ads")
    void testNoConsecutiveAds() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 3, 8);
        when(adsDeliveryPort.isHealthy()).thenReturn(true);
        when(adsDeliveryPort.requestPlacements(any()))
            .thenReturn(Optional.of(createSponsoredPlacements(2)));
        
        List<FeedItem> organic = createOrganicItems(20);
        List<FeedItem> result = adInjectionService.injectAds(organic, "user1", 0, "corr-1");
        
        // Check no two consecutive ads
        for (int i = 0; i < result.size() - 1; i++) {
            if (result.get(i).isSponsored()) {
                assertFalse(result.get(i + 1).isSponsored(), 
                    "Found consecutive ads at positions " + i + " and " + (i + 1));
            }
        }
    }

    @Test
    @DisplayName("Ads service returns empty - organic feed returned")
    void testAdsServiceReturnsEmpty() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 3, 8);
        when(adsDeliveryPort.isHealthy()).thenReturn(true);
        when(adsDeliveryPort.requestPlacements(any())).thenReturn(Optional.empty());
        
        List<FeedItem> organic = createOrganicItems(20);
        List<FeedItem> result = adInjectionService.injectAds(organic, "user1", 0, "corr-1");
        
        assertEquals(20, result.size());
        assertTrue(result.stream().allMatch(FeedItem::isOrganic));
    }

    @Test
    @DisplayName("Empty organic feed - returns empty")
    void testEmptyOrganicFeed() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 3, 8);
        
        List<FeedItem> result = adInjectionService.injectAds(List.of(), "user1", 0, "corr-1");
        
        assertTrue(result.isEmpty());
        verify(adsDeliveryPort, never()).requestPlacements(any());
    }

    @Test
    @DisplayName("Null organic feed - returns empty")
    void testNullOrganicFeed() {
        adInjectionService = new AdInjectionService(adsDeliveryPort, true, 3, 8);
        
        List<FeedItem> result = adInjectionService.injectAds(null, "user1", 0, "corr-1");
        
        assertTrue(result.isEmpty());
        verify(adsDeliveryPort, never()).requestPlacements(any());
    }

    private List<FeedItem> createOrganicItems(int count) {
        List<FeedItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(FeedItem.organic(
                UUID.randomUUID(),
                "author" + i,
                "Caption " + i,
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                List.of("hashtag" + i),
                Instant.now(),
                100 + i,
                10 + i,
                5 + i,
                90.0 - i
            ));
        }
        return items;
    }

    private List<SponsoredPlacement> createSponsoredPlacements(int count) {
        List<SponsoredPlacement> placements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            placements.add(new SponsoredPlacement(
                "delivery-" + i,
                "campaign-" + i,
                "EVENT",
                "event-" + i,
                Map.of("title", "Sponsored Event " + i),
                BigDecimal.valueOf(5.0),
                "token-" + i
            ));
        }
        return placements;
    }
}
