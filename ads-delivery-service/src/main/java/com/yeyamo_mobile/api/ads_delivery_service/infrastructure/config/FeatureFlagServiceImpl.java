package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.config;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.PlacementType;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.FeatureFlagService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FeatureFlagServiceImpl implements FeatureFlagService {

    @Value("${yeyamo.ads.feature-flags.ads-delivery-enabled:false}")
    private boolean adsDeliveryEnabled;

    @Value("${yeyamo.ads.feature-flags.ads-feed-enabled:false}")
    private boolean adsFeedEnabled;

    @Value("${yeyamo.ads.feature-flags.ads-discovery-enabled:false}")
    private boolean adsDiscoveryEnabled;

    @Value("${yeyamo.ads.feature-flags.ads-search-enabled:false}")
    private boolean adsSearchEnabled;

    @Value("${yeyamo.ads.feature-flags.ads-map-enabled:false}")
    private boolean adsMapEnabled;

    @Value("${yeyamo.ads.feature-flags.ads-event-highlight-enabled:false}")
    private boolean adsEventHighlightEnabled;

    @Value("${yeyamo.ads.feature-flags.ads-place-highlight-enabled:false}")
    private boolean adsPlaceHighlightEnabled;

    @Value("${yeyamo.ads.feature-flags.ads-partner-profile-banner-enabled:false}")
    private boolean adsPartnerProfileBannerEnabled;

    private final Map<PlacementType, Boolean> placementFlags = Map.of();

    @Override
    public boolean isEnabled(String flagName) {
        return switch (flagName) {
            case "ads_delivery_enabled" -> adsDeliveryEnabled;
            default -> false;
        };
    }

    @Override
    public boolean isPlacementEnabled(PlacementType placement) {
        return switch (placement) {
            case FEED_CARD -> adsFeedEnabled;
            case DISCOVERY_RESULT -> adsDiscoveryEnabled;
            case SEARCH_RESULT -> adsSearchEnabled;
            case MAP_PIN -> adsMapEnabled;
            case EVENT_HIGHLIGHT -> adsEventHighlightEnabled;
            case PLACE_HIGHLIGHT -> adsPlaceHighlightEnabled;
            case PARTNER_PROFILE_BANNER -> adsPartnerProfileBannerEnabled;
        };
    }
}
