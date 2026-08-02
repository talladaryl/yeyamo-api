package com.yeyamo_mobile.api.auth_service.dto;

import java.time.LocalDateTime;

public record SessionResponse(
        Long id,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        boolean active
) {
}
