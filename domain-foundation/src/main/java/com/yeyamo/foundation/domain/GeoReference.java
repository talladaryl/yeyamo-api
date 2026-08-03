package com.yeyamo.foundation.domain;

public record GeoReference(
        String countryCode,
        String adminLevel1Id,
        String adminLevel2Id,
        String cityId,
        String localityId,
        Double latitude,
        Double longitude) {
    public GeoReference {
        countryCode = Standards.countryCode(countryCode);
        adminLevel1Id = Standards.optional(adminLevel1Id);
        adminLevel2Id = Standards.optional(adminLevel2Id);
        cityId = Standards.optional(cityId);
        localityId = Standards.optional(localityId);
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}
