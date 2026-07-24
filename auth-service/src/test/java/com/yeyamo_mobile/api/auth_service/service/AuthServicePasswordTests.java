package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yeyamo_mobile.api.auth_service.dto.ChangePasswordRequest;
import com.yeyamo_mobile.api.auth_service.event.AuthEventOutbox;
import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.OAuthAccountRepository;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;
import com.yeyamo_mobile.api.auth_service.security.JwtService;

class AuthServicePasswordTests {
    private UserRepository users;
    private PasswordEncoder encoder;
    private RefreshTokenService refreshTokens;
    private AuthEventOutbox outbox;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        refreshTokens = mock(RefreshTokenService.class);
        outbox = mock(AuthEventOutbox.class);
        service = new AuthService(users, mock(OAuthAccountRepository.class), mock(RoleRepository.class),
                encoder, mock(AuthenticationManager.class), mock(JwtService.class), refreshTokens,
                mock(OAuthTokenVerifier.class), mock(OtpService.class), mock(EmailService.class),
                mock(LoginAttemptService.class), outbox);
    }

    @Test
    void changesPasswordAndRevokesRefreshTokens() {
        User user = user();
        when(encoder.matches("current-password", "old-hash")).thenReturn(true);
        when(encoder.matches("new-password-123", "old-hash")).thenReturn(false);
        when(encoder.encode("new-password-123")).thenReturn("new-hash");

        service.changePassword(user,
                new ChangePasswordRequest("current-password", "new-password-123"), "correlation");

        assertEquals("new-hash", user.getPasswordHash());
        verify(users).save(user);
        verify(refreshTokens).revokeAll(user);
        verify(outbox).passwordChanged(user, "correlation");
    }

    @Test
    void rejectsInvalidCurrentPassword() {
        User user = user();
        when(encoder.matches("wrong-password", "old-hash")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> service.changePassword(user,
                new ChangePasswordRequest("wrong-password", "new-password-123"), null));

        assertEquals("INVALID_CURRENT_PASSWORD", exception.getCode());
    }

    private User user() {
        User user = new User();
        user.setId(42L);
        user.setPasswordHash("old-hash");
        return user;
    }
}
