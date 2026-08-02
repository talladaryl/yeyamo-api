package com.yeyamo_mobile.api.ads_delivery_service.domain.port;

import com.yeyamo_mobile.api.ads_delivery_service.application.dto.TrackingTokenData;
import java.time.Instant;

public interface TrackingTokenService {
    String generateImpressionToken(String deliveryId, String campaignId, String userId, Instant expiresAt);
    String generateClickToken(String deliveryId, String campaignId, String userId, Instant expiresAt);
    boolean verifyImpressionToken(String token);
    boolean verifyClickToken(String token);
    TrackingTokenData decodeImpressionToken(String token);
    TrackingTokenData decodeClickToken(String token);
}
