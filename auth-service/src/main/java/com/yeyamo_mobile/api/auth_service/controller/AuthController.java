package com.yeyamo_mobile.api.auth_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.yeyamo_mobile.api.auth_service.dto.AuthResponse;
import com.yeyamo_mobile.api.auth_service.dto.EmailRequest;
import com.yeyamo_mobile.api.auth_service.dto.LoginRequest;
import com.yeyamo_mobile.api.auth_service.dto.MessageResponse;
import com.yeyamo_mobile.api.auth_service.dto.OAuthLoginRequest;
import com.yeyamo_mobile.api.auth_service.dto.OtpVerificationRequest;
import com.yeyamo_mobile.api.auth_service.dto.PasswordResetRequest;
import com.yeyamo_mobile.api.auth_service.dto.RefreshTokenRequest;
import com.yeyamo_mobile.api.auth_service.dto.RegisterRequest;
import com.yeyamo_mobile.api.auth_service.dto.UserResponse;
import com.yeyamo_mobile.api.auth_service.security.UserPrincipal;
import com.yeyamo_mobile.api.auth_service.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/oauth/google")
    public AuthResponse google(@Valid @RequestBody OAuthLoginRequest request) {
        return authService.oauthLogin("google", request);
    }

    @PostMapping("/oauth/apple")
    public AuthResponse apple(@Valid @RequestBody OAuthLoginRequest request) {
        return authService.oauthLogin("apple", request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/email/verification/request")
    public MessageResponse requestEmailVerification(@Valid @RequestBody EmailRequest request) {
        authService.requestEmailVerification(request);
        return new MessageResponse("Code de verification envoye");
    }

    @PostMapping("/email/verification/confirm")
    public MessageResponse confirmEmailVerification(@Valid @RequestBody OtpVerificationRequest request) {
        authService.confirmEmailVerification(request);
        return new MessageResponse("Email verifie avec succes");
    }

    @PostMapping("/password/forgot")
    public MessageResponse requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        authService.requestPasswordReset(request);
        return new MessageResponse("Code de reinitialisation envoye");
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Mot de passe reinitialise avec succes");
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal.user());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.user());
    }
}
