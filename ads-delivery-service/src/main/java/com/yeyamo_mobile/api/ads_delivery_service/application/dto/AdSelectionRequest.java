package com.yeyamo_mobile.api.ads_delivery_service.application.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record AdSelectionRequest(
    String userId,
    String anonymousSessionId,
    @NotBlank String placement,
    @NotBlank String countryCode,
    String regionId,
    String cityId,
    String districtId,
    Double latitude,
    Double longitude,
    List<String> interestIds,
    String ageBand,
    @NotBlank String language,
    @NotBlank String deviceType,
    @NotNull Instant requestTimestamp,
    String contextEntityType,
    String contextEntityId,
    List<String> excludedCampaignIds,
    @Min(1) @Max(20) Integer limit
) {
}
