package com.yeyamo_mobile.api.ads_delivery_service.domain.port;

public interface FrequencyCapService {
    boolean canShowAd(String userId, String campaignId, String timeWindow);
    void recordImpression(String userId, String campaignId);
}
