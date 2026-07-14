package com.yeyamo_mobile.api.auth_service.dto;

public record PasswordResetRequest(
        String email,
        String otp,
        String newPassword
) {
}
