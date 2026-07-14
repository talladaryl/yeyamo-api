package com.yeyamo_mobile.api.auth_service.service;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.auth_service.dto.AuthResponse;
import com.yeyamo_mobile.api.auth_service.dto.EmailRequest;
import com.yeyamo_mobile.api.auth_service.dto.LoginRequest;
import com.yeyamo_mobile.api.auth_service.dto.OAuthLoginRequest;
import com.yeyamo_mobile.api.auth_service.dto.OtpVerificationRequest;
import com.yeyamo_mobile.api.auth_service.dto.PasswordResetRequest;
import com.yeyamo_mobile.api.auth_service.dto.RefreshTokenRequest;
import com.yeyamo_mobile.api.auth_service.dto.RegisterRequest;
import com.yeyamo_mobile.api.auth_service.dto.UserResponse;
import com.yeyamo_mobile.api.auth_service.enums.LabelRole;
import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.event.AuthEventOutbox;
import com.yeyamo_mobile.api.auth_service.models.OAuthAccount;
import com.yeyamo_mobile.api.auth_service.models.RefreshToken;
import com.yeyamo_mobile.api.auth_service.models.Role;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.OAuthAccountRepository;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;
import com.yeyamo_mobile.api.auth_service.security.JwtService;
import com.yeyamo_mobile.api.auth_service.service.OAuthTokenVerifier.OAuthUserInfo;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OAuthTokenVerifier oAuthTokenVerifier;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AuthEventOutbox eventOutbox;

    public AuthService(
            UserRepository userRepository,
            OAuthAccountRepository oAuthAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            OAuthTokenVerifier oAuthTokenVerifier,
            OtpService otpService,
            EmailService emailService,
            AuthEventOutbox eventOutbox
    ) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.oAuthTokenVerifier = oAuthTokenVerifier;
        this.otpService = otpService;
        this.emailService = emailService;
        this.eventOutbox = eventOutbox;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (hasText(request.email()) && userRepository.existsByEmail(clean(request.email()))) {
            throw new ApiException("EMAIL_ALREADY_USED", "Cet email est déjà utilisé", HttpStatus.CONFLICT);
        }
        if (hasText(request.phone()) && userRepository.existsByPhone(clean(request.phone()))) {
            throw new ApiException("PHONE_ALREADY_USED", "Ce numéro de téléphone est déjà utilisé", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(clean(request.email()));
        user.setPhone(clean(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(hasText(request.email()) ? UserStatus.PENDING : UserStatus.ACTIVE);
        user.getRoles().add(defaultUserRole());

        User savedUser = userRepository.save(user);
        eventOutbox.userCreated(savedUser, null);
        if (hasText(savedUser.getEmail())) {
            sendEmailVerificationOtp(savedUser);
        }
        return tokensFor(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request == null || !hasText(request.identifier()) || !hasText(request.password())) {
            throw new ApiException("INVALID_CREDENTIALS", "Identifiant ou mot de passe invalide", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(clean(request.identifier()))
                .or(() -> userRepository.findByPhone(clean(request.identifier())))
                .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Identifiant ou mot de passe invalide", HttpStatus.UNAUTHORIZED));

        if (user.getStatus() == UserStatus.PENDING) {
            throw new ApiException("EMAIL_NOT_VERIFIED", "Veuillez verifier votre email avant de vous connecter", HttpStatus.FORBIDDEN);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(clean(request.identifier()), request.password())
        );

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return tokensFor(user);
    }

    @Transactional
    public AuthResponse oauthLogin(String provider, OAuthLoginRequest request) {
        OAuthUserInfo userInfo = oAuthTokenVerifier.verify(provider, request == null ? null : request.idToken());

        User user = oAuthAccountRepository
                .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId())
                .map(OAuthAccount::getUser)
                .orElseGet(() -> findOrCreateOAuthUser(userInfo));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return tokensFor(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (request == null || !hasText(request.refreshToken())) {
            throw new ApiException("INVALID_REFRESH_TOKEN", "Refresh token requis", HttpStatus.UNAUTHORIZED);
        }

        RefreshToken existing = refreshTokenService.verify(request.refreshToken());
        User user = existing.getUser();
        String newRefreshToken = refreshTokenService.rotate(request.refreshToken());
        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, newRefreshToken, jwtService.accessTokenExpirationMs() / 1000, toResponse(user));
    }

    @Transactional
    public void logout(User user) {
        refreshTokenService.revokeAll(user);
    }

    public UserResponse me(User user) {
        return toResponse(user);
    }

    public void requestEmailVerification(EmailRequest request) {
        User user = requireUserByEmail(request == null ? null : request.email());
        if (user.getEmailVerifiedAt() != null) {
            return;
        }
        sendEmailVerificationOtp(user);
    }

    @Transactional
    public void confirmEmailVerification(OtpVerificationRequest request) {
        User user = requireUserByEmail(request == null ? null : request.email());
        if (!hasText(request.otp())) {
            throw new ApiException("OTP_REQUIRED", "Code OTP requis", HttpStatus.BAD_REQUEST);
        }
        otpService.verify("email-verification", user.getEmail(), request.otp());
        user.setEmailVerifiedAt(Instant.now());
        if (user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    public void requestPasswordReset(EmailRequest request) {
        User user = requireUserByEmail(request == null ? null : request.email());
        String otp = otpService.generate("password-reset", user.getEmail());
        emailService.sendPasswordResetOtp(user.getEmail(), otp, otpService.expirationMinutes());
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = requireUserByEmail(request == null ? null : request.email());
        if (!hasText(request.otp())) {
            throw new ApiException("OTP_REQUIRED", "Code OTP requis", HttpStatus.BAD_REQUEST);
        }
        if (!hasText(request.newPassword()) || request.newPassword().length() < 8) {
            throw new ApiException("WEAK_PASSWORD", "Le mot de passe doit contenir au moins 8 caracteres", HttpStatus.BAD_REQUEST);
        }

        otpService.verify("password-reset", user.getEmail(), request.otp());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(user);
        userRepository.save(user);
    }

    private AuthResponse tokensFor(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken, jwtService.accessTokenExpirationMs() / 1000, toResponse(user));
    }

    private UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getCode().name())
                .collect(Collectors.toSet());

        return new UserResponse(user.getId(), user.getEmail(), user.getPhone(), user.getStatus(), roles, user.getCreatedAt(), user.getEmailVerifiedAt());
    }

    private Role defaultUserRole() {
        return roleRepository.findByCode(Roles.USER)
                .orElseGet(() -> roleRepository.save(new Role(null, Roles.USER, LabelRole.UTILISATEUR)));
    }

    private void sendEmailVerificationOtp(User user) {
        String otp = otpService.generate("email-verification", user.getEmail());
        emailService.sendEmailVerificationOtp(user.getEmail(), otp, otpService.expirationMinutes());
    }

    private User requireUserByEmail(String email) {
        if (!hasText(email)) {
            throw new ApiException("EMAIL_REQUIRED", "Email requis", HttpStatus.BAD_REQUEST);
        }
        return userRepository.findByEmail(clean(email))
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "Utilisateur introuvable", HttpStatus.NOT_FOUND));
    }

    private User findOrCreateOAuthUser(OAuthUserInfo userInfo) {
        User user = null;
        if (hasText(userInfo.email())) {
            user = userRepository.findByEmail(clean(userInfo.email())).orElse(null);
        }

        if (user == null) {
            user = new User();
            user.setEmail(clean(userInfo.email()));
            user.setPasswordHash(passwordEncoder.encode("oauth2:" + userInfo.provider() + ":" + userInfo.providerUserId()));
            user.setStatus(UserStatus.ACTIVE);
            if (hasText(userInfo.email())) {
                user.setEmailVerifiedAt(Instant.now());
            }
            user.getRoles().add(defaultUserRole());
            user = userRepository.save(user);
            eventOutbox.userCreated(user, null);
        }

        OAuthAccount account = new OAuthAccount();
        account.setUser(user);
        account.setProvider(userInfo.provider());
        account.setProviderUserId(userInfo.providerUserId());
        oAuthAccountRepository.save(account);

        return user;
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null || (!hasText(request.email()) && !hasText(request.phone()))) {
            throw new ApiException("IDENTIFIER_REQUIRED", "Email ou téléphone requis", HttpStatus.BAD_REQUEST);
        }
        if (!hasText(request.password()) || request.password().length() < 8) {
            throw new ApiException("WEAK_PASSWORD", "Le mot de passe doit contenir au moins 8 caractères", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim().toLowerCase() : null;
    }
}
