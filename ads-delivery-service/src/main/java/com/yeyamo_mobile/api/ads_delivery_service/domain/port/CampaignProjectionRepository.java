package com.yeyamo_mobile.api.ads_delivery_service.domain.port;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.CampaignProjection;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.PlacementType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CampaignProjectionRepository {
    List<CampaignProjection> findActiveCampaignsForPlacement(PlacementType placement, Instant now);
    Optional<CampaignProjection> findById(String campaignId);
    void save(CampaignProjection projection);
    void updateSpentAmount(String campaignId, java.math.BigDecimal newSpentAmount);
}
