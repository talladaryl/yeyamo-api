package com.yeyamo_mobile.api.country_config_service.dto;

import java.util.UUID;

public record TimezoneDto(
    UUID id,
    String timezone,
    String displayName,
    Boolean isDefault
) {
}
