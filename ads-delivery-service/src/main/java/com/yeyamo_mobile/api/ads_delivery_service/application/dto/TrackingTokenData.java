package com.yeyamo_mobile.api.ads_delivery_service.application.dto;

import java.time.Instant;

public record TrackingTokenData(
    String deliveryId,
    String campaignId,
    String userId,
    Instant expiresAt
) {
}
