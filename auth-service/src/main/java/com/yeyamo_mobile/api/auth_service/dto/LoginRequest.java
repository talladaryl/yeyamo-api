package com.yeyamo_mobile.api.auth_service.dto;

public record LoginRequest(
        String identifier,
        String password
) {
}
