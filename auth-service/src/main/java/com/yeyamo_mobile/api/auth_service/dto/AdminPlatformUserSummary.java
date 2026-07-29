package com.yeyamo_mobile.api.auth_service.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import com.yeyamo_mobile.api.auth_service.enums.UserStatus;

public record AdminPlatformUserSummary(
        Long id,
        String displayName,
        String username,
        String email,
        String phone,
        String avatarUrl,
        UserStatus status,
        Set<String> roles,
        Long regionId,
        LocalDateTime createdAt,
        Instant lastLoginAt) {
}
