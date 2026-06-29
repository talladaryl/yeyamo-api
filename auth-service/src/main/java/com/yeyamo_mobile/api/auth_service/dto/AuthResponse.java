package com.yeyamo_mobile.api.auth_service.dto;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
