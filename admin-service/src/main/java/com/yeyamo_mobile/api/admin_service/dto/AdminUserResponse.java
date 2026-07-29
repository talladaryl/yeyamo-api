package com.yeyamo_mobile.api.admin_service.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.yeyamo_mobile.api.admin_service.enums.AdminRole;
import com.yeyamo_mobile.api.admin_service.enums.AdminStatus;

public record AdminUserResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        Set<AdminRole> roles,
        Set<AdminPermissionResponse> permissions,
        Set<String> scopes,
        AdminStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt) {
}
