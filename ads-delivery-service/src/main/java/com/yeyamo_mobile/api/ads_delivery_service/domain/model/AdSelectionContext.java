package com.yeyamo_mobile.api.ads_delivery_service.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing the context for ad selection
 */
public final class AdSelectionContext {
    private final String userId;
    private final String anonymousSessionId;
    private final PlacementType placement;
    private final String countryCode;
    private final String regionId;
    private final String cityId;
    private final String districtId;
    private final Double latitude;
    private final Double longitude;
    private final List<String> interestIds;
    private final String ageBand;
    private final String language;
    private final String deviceType;
    private final Instant requestTimestamp;
    private final PromotedEntityType contextEntityType;
    private final String contextEntityId;
    private final List<String> excludedCampaignIds;
    private final int limit;

    private AdSelectionContext(Builder builder) {
        this.userId = builder.userId;
        this.anonymousSessionId = builder.anonymousSessionId;
        this.placement = Objects.requireNonNull(builder.placement, "placement cannot be null");
        this.countryCode = Objects.requireNonNull(builder.countryCode, "countryCode cannot be null");
        this.regionId = builder.regionId;
        this.cityId = builder.cityId;
        this.districtId = builder.districtId;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.interestIds = builder.interestIds != null ? List.copyOf(builder.interestIds) : List.of();
        this.ageBand = builder.ageBand;
        this.language = Objects.requireNonNull(builder.language, "language cannot be null");
        this.deviceType = Objects.requireNonNull(builder.deviceType, "deviceType cannot be null");
        this.requestTimestamp = Objects.requireNonNull(builder.requestTimestamp, "requestTimestamp cannot be null");
        this.contextEntityType = builder.contextEntityType;
        this.contextEntityId = builder.contextEntityId;
        this.excludedCampaignIds = builder.excludedCampaignIds != null ? List.copyOf(builder.excludedCampaignIds) : List.of();
        this.limit = builder.limit > 0 ? builder.limit : 10;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getAnonymousSessionId() { return anonymousSessionId; }
    public PlacementType getPlacement() { return placement; }
    public String getCountryCode() { return countryCode; }
    public String getRegionId() { return regionId; }
    public String getCityId() { return cityId; }
    public String getDistrictId() { return districtId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public List<String> getInterestIds() { return interestIds; }
    public String getAgeBand() { return ageBand; }
    public String getLanguage() { return language; }
    public String getDeviceType() { return deviceType; }
    public Instant getRequestTimestamp() { return requestTimestamp; }
    public PromotedEntityType getContextEntityType() { return contextEntityType; }
    public String getContextEntityId() { return contextEntityId; }
    public List<String> getExcludedCampaignIds() { return excludedCampaignIds; }
    public int getLimit() { return limit; }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public static class Builder {
        private String userId;
        private String anonymousSessionId;
        private PlacementType placement;
        private String countryCode;
        private String regionId;
        private String cityId;
        private String districtId;
        private Double latitude;
        private Double longitude;
        private List<String> interestIds;
        private String ageBand;
        private String language;
        private String deviceType;
        private Instant requestTimestamp;
        private PromotedEntityType contextEntityType;
        private String contextEntityId;
        private List<String> excludedCampaignIds;
        private int limit = 10;

        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder anonymousSessionId(String anonymousSessionId) { this.anonymousSessionId = anonymousSessionId; return this; }
        public Builder placement(PlacementType placement) { this.placement = placement; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode; return this; }
        public Builder regionId(String regionId) { this.regionId = regionId; return this; }
        public Builder cityId(String cityId) { this.cityId = cityId; return this; }
        public Builder districtId(String districtId) { this.districtId = districtId; return this; }
        public Builder latitude(Double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { this.longitude = longitude; return this; }
        public Builder interestIds(List<String> interestIds) { this.interestIds = interestIds; return this; }
        public Builder ageBand(String ageBand) { this.ageBand = ageBand; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public Builder requestTimestamp(Instant requestTimestamp) { this.requestTimestamp = requestTimestamp; return this; }
        public Builder contextEntityType(PromotedEntityType contextEntityType) { this.contextEntityType = contextEntityType; return this; }
        public Builder contextEntityId(String contextEntityId) { this.contextEntityId = contextEntityId; return this; }
        public Builder excludedCampaignIds(List<String> excludedCampaignIds) { this.excludedCampaignIds = excludedCampaignIds; return this; }
        public Builder limit(int limit) { this.limit = limit; return this; }

        public AdSelectionContext build() {
            return new AdSelectionContext(this);
        }
    }
}
