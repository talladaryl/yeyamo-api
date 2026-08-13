package com.yeyamo_mobile.api.country_config_service.dto;

import java.util.UUID;

public record LanguageDto(
    UUID id,
    String languageCode,
    String name,
    Boolean isDefault,
    Integer displayOrder
) {
}
