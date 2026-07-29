package com.yeyamo_mobile.api.admin_service.dto;

import java.util.Set;

import com.yeyamo_mobile.api.admin_service.enums.AdminRole;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull AdminRole role,
        @Size(max = 100) Set<String> permissions,
        @Size(max = 100) Set<String> scopes,
        @NotNull AdminStatus status) {
}
