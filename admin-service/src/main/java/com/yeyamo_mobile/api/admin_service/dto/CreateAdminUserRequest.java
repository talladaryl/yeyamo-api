package com.yeyamo_mobile.api.admin_service.dto;

import java.util.Set;
import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.AdminRole;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull AdminRole role,
        @Size(max = 100) Set<String> permissions,
        @Size(max = 100) Set<String> scopes,
        AdminStatus status) {
}
