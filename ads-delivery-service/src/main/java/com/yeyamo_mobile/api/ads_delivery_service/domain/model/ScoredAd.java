package com.yeyamo_mobile.api.ads_delivery_service.domain.model;

import java.util.List;

/**
 * Represents a campaign with its calculated selection score
 */
public class ScoredAd {
    private final CampaignProjection campaign;
    private final double score;
    private final List<String> reasonCodes;
    private final String policyVersion;

    public ScoredAd(CampaignProjection campaign, double score, List<String> reasonCodes, String policyVersion) {
        this.campaign = campaign;
        this.score = score;
        this.reasonCodes = List.copyOf(reasonCodes);
        this.policyVersion = policyVersion;
    }

    public CampaignProjection getCampaign() {
        return campaign;
    }

    public double getScore() {
        return score;
    }

    public List<String> getReasonCodes() {
        return reasonCodes;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }
}
