package com.yeyamo_mobile.api.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.models.User;

class SessionEligibilityPolicyTests {

    @Test
    void emailPendingIsNotEligible() {
        User user = user("user@example.com", UserStatus.PENDING, null);

        assertFalse(SessionEligibilityPolicy.isEligible(user));
        assertTrue(SessionEligibilityPolicy.requiresVerifiedEmail(user));
    }

    @Test
    void emailActiveButUnverifiedIsNotEligible() {
        User user = user("user@example.com", UserStatus.ACTIVE, null);

        assertFalse(SessionEligibilityPolicy.isEligible(user));
    }

    @Test
    void emailActiveAndVerifiedIsEligible() {
        User user = user("user@example.com", UserStatus.ACTIVE, Instant.now());

        assertTrue(SessionEligibilityPolicy.isEligible(user));
    }

    @Test
    void disabledStatusesAreNotEligible() {
        for (UserStatus status : new UserStatus[] {
                UserStatus.SUSPENDED, UserStatus.BLOCKED, UserStatus.DEACTIVATED,
                UserStatus.LOCKED, UserStatus.BANNED, UserStatus.INACTIVE, UserStatus.DELETED
        }) {
            assertFalse(SessionEligibilityPolicy.isEligible(user(null, status, null)), status.name());
        }
    }

    @Test
    void phoneOnlyActiveAccountRemainsEligible() {
        assertTrue(SessionEligibilityPolicy.isEligible(user(null, UserStatus.ACTIVE, null)));
    }

    private User user(String email, UserStatus status, Instant verifiedAt) {
        User user = new User();
        user.setEmail(email);
        user.setStatus(status);
        user.setEmailVerifiedAt(verifiedAt);
        return user;
    }
}
