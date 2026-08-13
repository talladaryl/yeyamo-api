package com.yeyamo_mobile.api.country_config_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LocalityDto(
    UUID id,
    String countryCode,
    UUID cityId,
    UUID administrativeAreaId,
    String localityType,
    String name,
    String slug,
    BigDecimal latitude,
    BigDecimal longitude,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
