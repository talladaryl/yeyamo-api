package com.yeyamo_mobile.api.auth_service.dto;

import java.util.Set;

import com.yeyamo_mobile.api.auth_service.enums.Roles;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AdminUserRolesRequest(
        @NotEmpty @Size(max = 5) Set<Roles> roles,
        @Size(max = 500) String reason) {
}
