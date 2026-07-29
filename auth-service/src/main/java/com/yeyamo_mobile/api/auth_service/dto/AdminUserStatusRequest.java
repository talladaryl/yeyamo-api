package com.yeyamo_mobile.api.auth_service.dto;

import com.yeyamo_mobile.api.auth_service.enums.UserStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserStatusRequest(
        @NotNull UserStatus status,
        @NotBlank @Size(max = 500) String reason) {
}
