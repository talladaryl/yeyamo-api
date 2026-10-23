package com.yeyamo_mobile.api.auth_service.security;

import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.enums.UserStatus;

public final class SessionEligibilityPolicy {

    private SessionEligibilityPolicy() {
    }

    public static boolean isEligible(User user) {
        return user != null
                && user.getStatus() == UserStatus.ACTIVE
                && (!hasEmail(user) || user.getEmailVerifiedAt() != null);
    }

    public static boolean requiresVerifiedEmail(User user) {
        return hasEmail(user) && user.getEmailVerifiedAt() == null;
    }

    private static boolean hasEmail(User user) {
        return user != null && user.getEmail() != null && !user.getEmail().isBlank();
    }
}
