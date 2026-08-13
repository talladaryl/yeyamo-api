package com.yeyamo_mobile.api.country_config_service.dto;

import java.util.UUID;

public record CurrencyDto(
    UUID id,
    String currencyCode,
    String name,
    String symbol,
    Integer decimalPlaces,
    Boolean isDefault
) {
}
