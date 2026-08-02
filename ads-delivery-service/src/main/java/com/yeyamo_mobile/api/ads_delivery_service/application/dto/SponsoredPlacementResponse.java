package com.yeyamo_mobile.api.ads_delivery_service.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SponsoredPlacementResponse(
    String deliveryId,
    String campaignId,
    String promotedEntityType,
    String promotedEntityId,
    String placement,
    Map<String, Object> creative,
    String disclosureLabel,
    List<String> reasonCodes,
    String impressionTrackingToken,
    String clickTrackingToken,
    Instant expiresAt,
    int rankPosition,
    String deliveryPolicyVersion
) {
}
