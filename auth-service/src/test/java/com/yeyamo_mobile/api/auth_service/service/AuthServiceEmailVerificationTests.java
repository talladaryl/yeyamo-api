package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;

import com.yeyamo_mobile.api.auth_service.dto.EmailVerificationResponse;
import com.yeyamo_mobile.api.auth_service.dto.OtpVerificationRequest;
import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.event.AuthEventOutbox;
import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.OAuthAccountRepository;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;
import com.yeyamo_mobile.api.auth_service.security.AntiBotVerifier;
import com.yeyamo_mobile.api.auth_service.security.JwtService;

class AuthServiceEmailVerificationTests {
    private UserRepository users;
    private OtpService otpService;
    private RefreshTokenService refreshTokens;
    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        otpService = mock(OtpService.class);
        refreshTokens = mock(RefreshTokenService.class);
        jwtService = mock(JwtService.class);
        service = new AuthService(users, mock(OAuthAccountRepository.class), mock(RoleRepository.class),
                mock(PasswordEncoder.class), mock(AuthenticationManager.class), jwtService, refreshTokens,
                mock(OAuthTokenVerifier.class), otpService, mock(EmailService.class),
                mock(LoginAttemptService.class), mock(AuthEventOutbox.class), mock(AntiBotVerifier.class));
    }

    @Test
    void activatesPendingAccountAndCreatesSessionAfterValidOtp() {
        User user = user(UserStatus.PENDING);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.accessTokenExpirationMs()).thenReturn(1_800_000L);
        when(refreshTokens.create(user)).thenReturn("refresh-token");

        EmailVerificationResponse response = service.confirmEmailVerification(
                new OtpVerificationRequest("user@example.com", "123456"));

        assertTrue(response.authenticated());
        assertTrue(response.accessToken() != null && response.refreshToken() != null);
        assertTrue(user.getStatus() == UserStatus.ACTIVE);
        verify(users).save(user);
    }

    @Test
    void doesNotCreateImplicitSessionForAlreadyActiveAccount() {
        User user = user(UserStatus.ACTIVE);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        EmailVerificationResponse response = service.confirmEmailVerification(
                new OtpVerificationRequest("user@example.com", "123456"));

        assertFalse(response.authenticated());
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokens, never()).create(any());
    }

    @Test
    void invalidOtpDoesNotActivateOrCreateSession() {
        User user = user(UserStatus.PENDING);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        doThrow(new ApiException("OTP_INVALID", "invalid", HttpStatus.BAD_REQUEST))
                .when(otpService).verify("email-verification", "user@example.com", "123456");

        assertThrows(ApiException.class, () -> service.confirmEmailVerification(
                new OtpVerificationRequest("user@example.com", "123456")));

        assertTrue(user.getStatus() == UserStatus.PENDING);
        verify(users, never()).save(any());
        verify(refreshTokens, never()).create(any());
    }

    @Test
    void expiredOtpDoesNotActivateOrCreateSession() {
        User user = user(UserStatus.PENDING);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        doThrow(new ApiException("OTP_EXPIRED", "expired", HttpStatus.BAD_REQUEST))
                .when(otpService).verify("email-verification", "user@example.com", "123456");

        assertThrows(ApiException.class, () -> service.confirmEmailVerification(
                new OtpVerificationRequest("user@example.com", "123456")));

        assertTrue(user.getStatus() == UserStatus.PENDING);
        verify(users, never()).save(any());
        verify(refreshTokens, never()).create(any());
    }

    @Test
    void consumedOtpCannotCreateASecondSession() {
        User user = user(UserStatus.PENDING);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.accessTokenExpirationMs()).thenReturn(1_800_000L);
        when(refreshTokens.create(user)).thenReturn("refresh-token");
        doNothing().doThrow(new ApiException("OTP_INVALID", "consumed", HttpStatus.BAD_REQUEST))
                .when(otpService).verify("email-verification", "user@example.com", "123456");

        assertTrue(service.confirmEmailVerification(
                new OtpVerificationRequest("user@example.com", "123456")).authenticated());
        assertThrows(ApiException.class, () -> service.confirmEmailVerification(
                new OtpVerificationRequest("user@example.com", "123456")));

        verify(refreshTokens, times(1)).create(user);
    }

    @Test
    void disabledAccountNeverGetsAnImplicitSession() {
        User user = user(UserStatus.SUSPENDED);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        EmailVerificationResponse response = service.confirmEmailVerification(
                new OtpVerificationRequest("user@example.com", "123456"));

        assertFalse(response.authenticated());
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokens, never()).create(any());
    }

    private User user(UserStatus status) {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setStatus(status);
        return user;
    }
}
