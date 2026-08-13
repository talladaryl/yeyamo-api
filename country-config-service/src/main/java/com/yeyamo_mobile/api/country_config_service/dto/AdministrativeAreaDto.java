package com.yeyamo_mobile.api.country_config_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdministrativeAreaDto(
    UUID id,
    String countryCode,
    UUID parentId,
    Integer level,
    String typeCode,
    String name,
    Map<String, String> localizedNames,
    String officialCode,
    String slug,
    BigDecimal latitude,
    BigDecimal longitude,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
