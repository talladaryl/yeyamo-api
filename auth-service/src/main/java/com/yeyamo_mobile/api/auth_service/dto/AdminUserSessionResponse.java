package com.yeyamo_mobile.api.auth_service.dto;

import java.time.LocalDateTime;

public record AdminUserSessionResponse(
        Long sessionId,
        String device,
        String ipMasked,
        LocalDateTime createdAt,
        LocalDateTime lastSeenAt,
        LocalDateTime expiresAt,
        boolean active) {
}
