package com.yeyamo_mobile.api.country_config_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CityDto(
    UUID id,
    String countryCode,
    UUID administrativeAreaId,
    String name,
    String slug,
    BigDecimal latitude,
    BigDecimal longitude,
    Boolean active,
    Long population,
    Instant createdAt,
    Instant updatedAt
) {
}
