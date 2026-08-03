package com.yeyamo.foundation.domain;

public record AdministrativeAreaReference(String countryCode, int level, String areaId) {
    public AdministrativeAreaReference {
        countryCode = Standards.countryCode(countryCode);
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("level must be between 1 and 3");
        }
        areaId = Standards.required(areaId, "areaId");
    }
}
