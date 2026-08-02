package com.yeyamo_mobile.api.ads_delivery_service.domain.port;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.PlacementType;

public interface FeatureFlagService {
    boolean isEnabled(String flagName);
    boolean isPlacementEnabled(PlacementType placement);
}
