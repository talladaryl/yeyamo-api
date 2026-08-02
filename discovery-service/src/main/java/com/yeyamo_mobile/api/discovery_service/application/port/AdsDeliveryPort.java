package com.yeyamo_mobile.api.discovery_service.application.port;

import java.util.List;
import java.util.Optional;

public interface AdsDeliveryPort {
    Optional<List<SponsoredPlacement>> requestPlacements(AdSelectionRequest request);
    boolean isHealthy();
}
