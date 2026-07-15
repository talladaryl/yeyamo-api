package com.yeyamo_mobile.api.auth_service.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

@Service
public class OAuthTokenVerifier {

    private final String googleClientId;
    private final String appleClientId;
    private final JwtDecoder googleDecoder;
    private final JwtDecoder appleDecoder;

    @Autowired
    public OAuthTokenVerifier(
            @Value("${oauth.google.client-id}") String googleClientId,
            @Value("${oauth.apple.client-id}") String appleClientId
    ) {
        this(googleClientId, appleClientId,
                NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build(),
                NimbusJwtDecoder.withJwkSetUri("https://appleid.apple.com/auth/keys").build());
    }

    OAuthTokenVerifier(String googleClientId, String appleClientId, JwtDecoder googleDecoder, JwtDecoder appleDecoder) {
        this.googleClientId = requiredClientId(googleClientId, "oauth.google.client-id");
        this.appleClientId = requiredClientId(appleClientId, "oauth.apple.client-id");
        this.googleDecoder = googleDecoder;
        this.appleDecoder = appleDecoder;
    }

    public OAuthUserInfo verify(String provider, String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException("OAUTH_TOKEN_REQUIRED", "Token OAuth requis", HttpStatus.BAD_REQUEST);
        }

        String normalizedProvider = provider.toLowerCase(Locale.ROOT);
        Jwt jwt = switch (normalizedProvider) {
            case "google" -> decode(googleDecoder, idToken, "GOOGLE_TOKEN_INVALID");
            case "apple" -> decode(appleDecoder, idToken, "APPLE_TOKEN_INVALID");
            default -> throw new ApiException("OAUTH_PROVIDER_UNSUPPORTED", "Provider OAuth non supporte", HttpStatus.BAD_REQUEST);
        };

        validateIssuer(normalizedProvider, jwt);
        validateAudience(normalizedProvider, jwt);

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (subject == null || subject.isBlank()) {
            throw new ApiException("OAUTH_SUBJECT_MISSING", "Identifiant OAuth introuvable", HttpStatus.UNAUTHORIZED);
        }
        if (email != null && !email.isBlank() && !emailVerified(jwt)) {
            throw new ApiException("OAUTH_EMAIL_UNVERIFIED", "Email OAuth non vérifié", HttpStatus.UNAUTHORIZED);
        }

        return new OAuthUserInfo(normalizedProvider, subject, email);
    }

    private Jwt decode(JwtDecoder decoder, String idToken, String code) {
        try {
            return decoder.decode(idToken);
        } catch (JwtException exception) {
            throw new ApiException(code, "Token OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateIssuer(String provider, Jwt jwt) {
        String expectedIssuer = switch (provider) {
            case "google" -> "https://accounts.google.com";
            case "apple" -> "https://appleid.apple.com";
            default -> "";
        };

        if (jwt.getIssuer() == null || !expectedIssuer.equals(jwt.getIssuer().toString())) {
            throw new ApiException("OAUTH_ISSUER_INVALID", "Issuer OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateAudience(String provider, Jwt jwt) {
        String expectedAudience = switch (provider) {
            case "google" -> googleClientId;
            case "apple" -> appleClientId;
            default -> "";
        };

        List<String> audiences = jwt.getAudience();
        if (!audiences.contains(expectedAudience)) {
            throw new ApiException("OAUTH_AUDIENCE_INVALID", "Audience OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
        if (audiences.size() > 1 && !expectedAudience.equals(jwt.getClaimAsString("azp"))) {
            throw new ApiException("OAUTH_AUTHORIZED_PARTY_INVALID", "Authorized party OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean emailVerified(Jwt jwt) {
        Object claim = jwt.getClaims().get("email_verified");
        return Boolean.TRUE.equals(claim) || claim instanceof String value && Boolean.parseBoolean(value);
    }

    private static String requiredClientId(String value, String property) {
        if (value == null || value.isBlank() || value.startsWith("your-")) {
            throw new IllegalArgumentException(property + " must be configured with a real OAuth client id");
        }
        return value.trim();
    }

    public record OAuthUserInfo(String provider, String providerUserId, String email) {
    }
}
