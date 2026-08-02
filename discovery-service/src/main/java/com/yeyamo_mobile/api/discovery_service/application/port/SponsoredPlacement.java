package com.yeyamo_mobile.api.discovery_service.application.port;

import java.math.BigDecimal;
import java.util.Map;

public record SponsoredPlacement(
    String deliveryId,
    String campaignId,
    String promotedEntityType,
    String promotedEntityId,
    Map<String, Object> creative,
    BigDecimal bidAmount,
    String trackingToken
) {}
