package com.yeyamo_mobile.api.admin_service.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.AdminRole;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;

import jakarta.validation.constraints.NotNull;

public record AdminUserRequest(
        @NotNull UUID userId,
        @NotNull AdminRole role,
        Map<String, Object> permissions,
        AdminStatus status
) {
    public Map<String, Object> safePermissions() {
        return permissions == null ? new LinkedHashMap<>() : permissions;
    }
}
