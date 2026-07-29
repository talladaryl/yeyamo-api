package com.yeyamo_mobile.api.auth_service.security;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TurnstileSiteVerifyResponse(
        boolean success,
        String hostname,
        String action,
        @JsonProperty("challenge_ts") String challengeTimestamp,
        @JsonProperty("error-codes") List<String> errorCodes
) {
}
