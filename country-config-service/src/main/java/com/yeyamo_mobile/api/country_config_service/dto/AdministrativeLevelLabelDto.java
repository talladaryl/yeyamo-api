package com.yeyamo_mobile.api.country_config_service.dto;

import java.util.Map;
import java.util.UUID;

public record AdministrativeLevelLabelDto(
    UUID id,
    String countryCode,
    Integer level,
    String label,
    String labelPlural,
    Map<String, String> localizedLabels,
    Integer displayOrder
) {
}
