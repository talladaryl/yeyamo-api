package com.yeyamo_mobile.api.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.models.RefreshToken;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.RefreshTokenRepository;
import com.yeyamo_mobile.api.auth_service.dto.SessionResponse;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String create(User user) {
        String rawToken = randomToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    public RefreshToken verify(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException("INVALID_REFRESH_TOKEN", "Refresh token invalide", HttpStatus.UNAUTHORIZED));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("INVALID_REFRESH_TOKEN", "Refresh token expiré ou révoqué", HttpStatus.UNAUTHORIZED);
        }

        return refreshToken;
    }

    public void revoke(RefreshToken refreshToken) {
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public java.util.List<SessionResponse> sessions(User user) {
        LocalDateTime now = LocalDateTime.now();
        return refreshTokenRepository.findByUserOrderByIdDesc(user).stream()
                .map(token -> new SessionResponse(
                        token.getId(),
                        token.getExpiresAt(),
                        token.getRevokedAt(),
                        token.getRevokedAt() == null && token.getExpiresAt().isAfter(now)))
                .toList();
    }

    @Transactional
    public void revoke(User user, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ApiException(
                        "SESSION_NOT_FOUND", "Session introuvable", HttpStatus.NOT_FOUND));
        if (token.getRevokedAt() == null) {
            revoke(token);
        }
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findLockedByTokenHash(hash(rawToken))
                .orElseThrow(() -> invalidToken("Refresh token invalide"));
        if (existing.getRevokedAt() != null) {
            refreshTokenRepository.deleteByUser(existing.getUser());
            throw new ApiException("REFRESH_TOKEN_REUSE_DETECTED",
                    "Réutilisation de refresh token détectée", HttpStatus.UNAUTHORIZED);
        }
        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalidToken("Refresh token expiré");
        }
        User user = existing.getUser();
        revoke(existing);
        return new Rotation(user, create(user));
    }

    public record Rotation(User user, String rawToken) {
    }

    private String randomToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible", exception);
        }
    }

    private ApiException invalidToken(String message) {
        return new ApiException("INVALID_REFRESH_TOKEN", message, HttpStatus.UNAUTHORIZED);
    }
}
