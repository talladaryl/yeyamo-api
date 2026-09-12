package com.yeyamo_mobile.api.event_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

public class EventRequest {

    private UUID placeId;

    @Size(max = 255)
    private String locationName;

    @Size(max = 500)
    private String locationAddress;

    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double locationLatitude;

    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double locationLongitude;

    private com.yeyamo_mobile.api.event_service.enums.EventVisibility visibility = com.yeyamo_mobile.api.event_service.enums.EventVisibility.PUBLIC;
    private boolean allowUninvitedParticipants = true;
    private boolean commentsParticipantsOnly;
    private boolean showParticipants = true;
    private boolean sharingEnabled = true;

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    private UUID coverMediaId;

    @NotNull
    @Future
    private Instant startAt;

    @NotNull
    private Instant endAt;

    @NotNull
    @Min(1)
    private Integer capacity;

    private EventStatus status;

    private boolean virtual;
    private java.util.Set<@Pattern(regexp = "[A-Z]{2}") String> accessibleCountries;
    @Pattern(regexp = "[A-Z]{2}", message = "countryCode must be ISO 3166-1 alpha-2") private String countryCode;
    private UUID adminLevel1Id;
    private UUID adminLevel2Id;
    private UUID cityId;
    private UUID localityId;
    @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
    @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
    @Size(max = 10) private String languageCode;

    public UUID getPlaceId() {
        return placeId;
    }

    public void setPlaceId(UUID placeId) {
        this.placeId = placeId;
    }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public Double getLocationLatitude() { return locationLatitude; }
    public void setLocationLatitude(Double locationLatitude) { this.locationLatitude = locationLatitude; }
    public Double getLocationLongitude() { return locationLongitude; }
    public void setLocationLongitude(Double locationLongitude) { this.locationLongitude = locationLongitude; }
    public com.yeyamo_mobile.api.event_service.enums.EventVisibility getVisibility() { return visibility; }
    public void setVisibility(com.yeyamo_mobile.api.event_service.enums.EventVisibility visibility) { this.visibility = visibility; }
    public boolean isAllowUninvitedParticipants() { return allowUninvitedParticipants; }
    public void setAllowUninvitedParticipants(boolean allowUninvitedParticipants) { this.allowUninvitedParticipants = allowUninvitedParticipants; }
    public boolean isCommentsParticipantsOnly() { return commentsParticipantsOnly; }
    public void setCommentsParticipantsOnly(boolean commentsParticipantsOnly) { this.commentsParticipantsOnly = commentsParticipantsOnly; }
    public boolean isShowParticipants() { return showParticipants; }
    public void setShowParticipants(boolean showParticipants) { this.showParticipants = showParticipants; }
    public boolean isSharingEnabled() { return sharingEnabled; }
    public void setSharingEnabled(boolean sharingEnabled) { this.sharingEnabled = sharingEnabled; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(UUID coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
    public boolean isVirtual() { return virtual; }
    public void setVirtual(boolean virtual) { this.virtual = virtual; }
    public java.util.Set<String> getAccessibleCountries() { return accessibleCountries; }
    public void setAccessibleCountries(java.util.Set<String> accessibleCountries) { this.accessibleCountries = accessibleCountries; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public UUID getAdminLevel1Id() { return adminLevel1Id; }
    public void setAdminLevel1Id(UUID adminLevel1Id) { this.adminLevel1Id = adminLevel1Id; }
    public UUID getAdminLevel2Id() { return adminLevel2Id; }
    public void setAdminLevel2Id(UUID adminLevel2Id) { this.adminLevel2Id = adminLevel2Id; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public UUID getLocalityId() { return localityId; }
    public void setLocalityId(UUID localityId) { this.localityId = localityId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
}
