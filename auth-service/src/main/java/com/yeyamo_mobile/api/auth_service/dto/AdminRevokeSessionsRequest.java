package com.yeyamo_mobile.api.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRevokeSessionsRequest(
        Long sessionId,
        @NotBlank @Size(max = 500) String reason) {
}
