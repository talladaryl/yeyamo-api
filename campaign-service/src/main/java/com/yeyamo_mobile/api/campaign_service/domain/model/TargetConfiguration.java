package com.yeyamo_mobile.api.campaign_service.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TargetConfiguration {
    private List<String> countryCodes;
    private List<String> regionIds;
    private List<String> cityIds;
    private List<String> districtIds;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private Integer minimumAge;
    private Integer maximumAge;
    private List<String> interestIds;
    private List<String> categoryIds;
    private List<String> languageCodes;
    private List<String> activeDays;
    private Integer startHour;
    private Integer endHour;
    private List<String> excludedUserIds;
    private Integer frequencyCapPerUserPerDay;
    private Integer frequencyCapPerUserTotal;

    public TargetConfiguration() {
        this.countryCodes = Collections.emptyList();
        this.regionIds = Collections.emptyList();
        this.cityIds = Collections.emptyList();
        this.districtIds = Collections.emptyList();
        this.interestIds = Collections.emptyList();
        this.categoryIds = Collections.emptyList();
        this.languageCodes = Collections.emptyList();
        this.activeDays = Collections.emptyList();
        this.excludedUserIds = Collections.emptyList();
    }

    public void validate() {
        if (minimumAge != null && minimumAge < 0) {
            throw new IllegalArgumentException("minimumAge cannot be negative");
        }
        if (maximumAge != null && maximumAge < 0) {
            throw new IllegalArgumentException("maximumAge cannot be negative");
        }
        if (minimumAge != null && maximumAge != null && minimumAge > maximumAge) {
            throw new IllegalArgumentException("minimumAge cannot be greater than maximumAge");
        }
        if (radiusKm != null && radiusKm <= 0) {
            throw new IllegalArgumentException("radiusKm must be positive");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        if (startHour != null && (startHour < 0 || startHour > 23)) {
            throw new IllegalArgumentException("startHour must be between 0 and 23");
        }
        if (endHour != null && (endHour < 0 || endHour > 23)) {
            throw new IllegalArgumentException("endHour must be between 0 and 23");
        }
        if (frequencyCapPerUserPerDay != null && frequencyCapPerUserPerDay <= 0) {
            throw new IllegalArgumentException("frequencyCapPerUserPerDay must be positive");
        }
        if (frequencyCapPerUserTotal != null && frequencyCapPerUserTotal <= 0) {
            throw new IllegalArgumentException("frequencyCapPerUserTotal must be positive");
        }
    }

    // Getters and Setters
    public List<String> getCountryCodes() { return countryCodes; }
    public void setCountryCodes(List<String> countryCodes) { this.countryCodes = countryCodes; }
    public List<String> getRegionIds() { return regionIds; }
    public void setRegionIds(List<String> regionIds) { this.regionIds = regionIds; }
    public List<String> getCityIds() { return cityIds; }
    public void setCityIds(List<String> cityIds) { this.cityIds = cityIds; }
    public List<String> getDistrictIds() { return districtIds; }
    public void setDistrictIds(List<String> districtIds) { this.districtIds = districtIds; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(Double radiusKm) { this.radiusKm = radiusKm; }
    public Integer getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Integer minimumAge) { this.minimumAge = minimumAge; }
    public Integer getMaximumAge() { return maximumAge; }
    public void setMaximumAge(Integer maximumAge) { this.maximumAge = maximumAge; }
    public List<String> getInterestIds() { return interestIds; }
    public void setInterestIds(List<String> interestIds) { this.interestIds = interestIds; }
    public List<String> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<String> categoryIds) { this.categoryIds = categoryIds; }
    public List<String> getLanguageCodes() { return languageCodes; }
    public void setLanguageCodes(List<String> languageCodes) { this.languageCodes = languageCodes; }
    public List<String> getActiveDays() { return activeDays; }
    public void setActiveDays(List<String> activeDays) { this.activeDays = activeDays; }
    public Integer getStartHour() { return startHour; }
    public void setStartHour(Integer startHour) { this.startHour = startHour; }
    public Integer getEndHour() { return endHour; }
    public void setEndHour(Integer endHour) { this.endHour = endHour; }
    public List<String> getExcludedUserIds() { return excludedUserIds; }
    public void setExcludedUserIds(List<String> excludedUserIds) { this.excludedUserIds = excludedUserIds; }
    public Integer getFrequencyCapPerUserPerDay() { return frequencyCapPerUserPerDay; }
    public void setFrequencyCapPerUserPerDay(Integer frequencyCapPerUserPerDay) { this.frequencyCapPerUserPerDay = frequencyCapPerUserPerDay; }
    public Integer getFrequencyCapPerUserTotal() { return frequencyCapPerUserTotal; }
    public void setFrequencyCapPerUserTotal(Integer frequencyCapPerUserTotal) { this.frequencyCapPerUserTotal = frequencyCapPerUserTotal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetConfiguration that = (TargetConfiguration) o;
        return Objects.equals(countryCodes, that.countryCodes) &&
                Objects.equals(regionIds, that.regionIds) &&
                Objects.equals(cityIds, that.cityIds) &&
                Objects.equals(districtIds, that.districtIds) &&
                Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude) &&
                Objects.equals(radiusKm, that.radiusKm) &&
                Objects.equals(minimumAge, that.minimumAge) &&
                Objects.equals(maximumAge, that.maximumAge) &&
                Objects.equals(interestIds, that.interestIds) &&
                Objects.equals(categoryIds, that.categoryIds) &&
                Objects.equals(languageCodes, that.languageCodes) &&
                Objects.equals(activeDays, that.activeDays) &&
                Objects.equals(startHour, that.startHour) &&
                Objects.equals(endHour, that.endHour);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCodes, regionIds, cityIds, districtIds, latitude, longitude, radiusKm,
                minimumAge, maximumAge, interestIds, categoryIds, languageCodes, activeDays, startHour, endHour);
    }
}
