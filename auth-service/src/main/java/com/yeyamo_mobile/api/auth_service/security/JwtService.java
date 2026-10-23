package com.yeyamo_mobile.api.auth_service.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.enums.Roles;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final int MINIMUM_HMAC_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final List<SecretKey> validationKeys;
    private final long accessTokenExpirationMs;
    private final String issuer;
    private final String audience;
    private final String keyId;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.previous-secrets:}") String previousSecrets,
            @Value("${jwt.access-token-expiration}") long accessTokenExpirationMs,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience,
            @Value("${jwt.key-id}") String keyId
    ) {
        this.signingKey = key(secret, "jwt.secret");
        this.validationKeys = new ArrayList<>();
        this.validationKeys.add(signingKey);
        Arrays.stream(previousSecrets.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> key(value, "jwt.previous-secrets"))
                .forEach(validationKeys::add);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.issuer = requireText(issuer, "jwt.issuer");
        this.audience = requireText(audience, "jwt.audience");
        this.keyId = requireText(keyId, "jwt.key-id");
        if (accessTokenExpirationMs <= 0) {
            throw new IllegalArgumentException("jwt.access-token-expiration must be positive");
        }
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode().name())
                .toList();
        Set<Roles> roleCodes = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> scopes = RoleAuthorities.scopes(roleCodes);
        Set<String> permissions = RoleAuthorities.permissions(roleCodes);

        return Jwts.builder()
                .header().keyId(keyId).and()
                .subject(String.valueOf(user.getId()))
                .claim("token_type", "access")
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("email", user.getEmail())
                .claim("phone", user.getPhone())
                .claim("country", user.getCountryCode())
                .claim("email_verified", user.getEmail() == null || user.getEmailVerifiedAt() != null)
                .claim("account_status", user.getStatus().name())
                .claim("roles", roles)
                .claim("scope", String.join(" ", scopes))
                .claim("scopes", scopes)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractAllClaims(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            return false;
        }
        try {
            Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            return false;
        }

        String tokenType = claims.get("token_type", String.class);
        if (StringUtils.hasText(tokenType) && !"access".equals(tokenType)) {
            return false;
        }

        String email = claims.get("email", String.class);
        if (StringUtils.hasText(email)) {
            if (!Boolean.TRUE.equals(claims.get("email_verified"))
                    || !"ACTIVE".equals(claims.get("account_status", String.class))) {
                return false;
            }
        }
        String accountStatus = claims.get("account_status", String.class);
        return !StringUtils.hasText(accountStatus) || "ACTIVE".equals(accountStatus);
    }

    public long accessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    private Claims extractAllClaims(String token) {
        JwtException lastFailure = null;
        for (SecretKey validationKey : validationKeys) {
            try {
                return Jwts.parser()
                        .verifyWith(validationKey)
                        .requireIssuer(issuer)
                        .requireAudience(audience)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (JwtException failure) {
                lastFailure = failure;
            }
        }
        throw lastFailure == null ? new JwtException("Invalid JWT") : lastFailure;
    }

    private static SecretKey key(String value, String property) {
        String secret = requireText(value, property);
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MINIMUM_HMAC_KEY_BYTES) {
            throw new IllegalArgumentException(property + " must contain at least 32 UTF-8 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static String requireText(String value, String property) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(property + " must be configured");
        }
        return value.trim();
    }
}
