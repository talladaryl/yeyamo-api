package com.yeyamo_mobile.api.place_service.dto;

import java.util.List;
import java.util.UUID;

import com.yeyamo_mobile.api.place_service.enums.MediaType;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PlaceRequest {

    private UUID partnerId;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long regionId;

    @NotNull
    private UUID cityId;

    private Long districtId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String slug;

    private String description;

    @NotNull
    @Min(-90)
    @Max(90)
    private Double latitude;

    @NotNull
    @Min(-180)
    @Max(180)
    private Double longitude;

    private String address;

    private String phone;

    private String website;

    private PlaceStatus status;

    @Valid
    private List<PlaceMediaRequest> media;

    @Valid
    private List<PlaceScheduleRequest> schedules;

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public UUID getCityId() {
        return cityId;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public PlaceStatus getStatus() {
        return status;
    }

    public void setStatus(PlaceStatus status) {
        this.status = status;
    }

    public List<PlaceMediaRequest> getMedia() {
        return media;
    }

    public void setMedia(List<PlaceMediaRequest> media) {
        this.media = media;
    }

    public List<PlaceScheduleRequest> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<PlaceScheduleRequest> schedules) {
        this.schedules = schedules;
    }

    public static class PlaceMediaRequest {

        @NotBlank
        private String url;

        @NotNull
        private MediaType type;

        private Integer displayOrder = 0;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public MediaType getType() {
            return type;
        }

        public void setType(MediaType type) {
            this.type = type;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
    }

    public static class PlaceScheduleRequest {

        @NotNull
        @Min(1)
        @Max(7)
        private Integer dayOfWeek;

        @NotNull
        private java.time.LocalTime openTime;

        @NotNull
        private java.time.LocalTime closeTime;

        public Integer getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(Integer dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public java.time.LocalTime getOpenTime() {
            return openTime;
        }

        public void setOpenTime(java.time.LocalTime openTime) {
            this.openTime = openTime;
        }

        public java.time.LocalTime getCloseTime() {
            return closeTime;
        }

        public void setCloseTime(java.time.LocalTime closeTime) {
            this.closeTime = closeTime;
        }
    }
}
