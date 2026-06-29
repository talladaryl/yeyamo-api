package com.yeyamo_mobile.api.auth_service.dto;

public record RegisterRequest(
        String email,
        String phone,
        String password,
        String displayName
) {
}
