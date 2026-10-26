package com.yeyamo_mobile.api.auth_service.dto;

public record EmailVerificationResponse(
        String message,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        boolean authenticated
) {
    public static EmailVerificationResponse confirmed(UserResponse user) {
        return new EmailVerificationResponse(
                "Email verifie avec succes", null, null, "Bearer", 0, user, false);
    }

    public static EmailVerificationResponse authenticated(AuthResponse session) {
        return new EmailVerificationResponse(
                "Email verifie avec succes", session.accessToken(), session.refreshToken(),
                session.tokenType(), session.expiresIn(), session.user(), true);
    }
}
