package com.yeyamo_mobile.api.country_config_service.dto;

import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLaunchStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLaunchStatusRequest(
    @NotNull CountryLaunchStatus launchStatus
) {
}
