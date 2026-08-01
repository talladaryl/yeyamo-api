package com.yeyamo_mobile.api.auth_service.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import com.yeyamo_mobile.api.auth_service.enums.UserStatus;

public record UserResponse(
        Long id,
        String email,
        String phone,
        UserStatus status,
        Set<String> roles,
        Set<String> permissions,
        Set<String> scopes,
        LocalDateTime createdAt,
        Instant emailVerifiedAt
) {
}
