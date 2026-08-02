package com.yeyamo_mobile.api.feed_service.application.port;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Sponsored content placement returned by ads-delivery-service
 */
public record SponsoredPlacement(
    String deliveryId,
    String campaignId,
    String promotedEntityType,
    String promotedEntityId,
    Map<String, Object> creative,
    BigDecimal bidAmount,
    String trackingToken
) {}
