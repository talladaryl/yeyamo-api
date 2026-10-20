package com.yeyamo_mobile.api.auth_service.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

@Service
public class OAuthTokenVerifier {

    private final Set<String> googleClientIds;
    private final String appleClientId;
    private final String microsoftClientId;
    private final String microsoftTenantId;
    private final JwtDecoder googleDecoder;
    private final JwtDecoder appleDecoder;
    private final JwtDecoder microsoftDecoder;

    @Autowired
    public OAuthTokenVerifier(
            @Value("${oauth.google.client-ids:}") String googleClientIds,
            @Value("${oauth.google.client-id:}") String googleClientId,
            @Value("${oauth.apple.client-id:}") String appleClientId,
            @Value("${oauth.microsoft.client-id:}") String microsoftClientId,
            @Value("${oauth.microsoft.tenant-id:common}") String microsoftTenantId
    ) {
        this(googleClientIds, googleClientId, appleClientId, microsoftClientId, microsoftTenantId,
                NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build(),
                NimbusJwtDecoder.withJwkSetUri("https://appleid.apple.com/auth/keys").build(),
                NimbusJwtDecoder.withJwkSetUri("https://login.microsoftonline.com/" + microsoftTenantId + "/discovery/v2.0/keys").build());
    }

    OAuthTokenVerifier(String googleClientId, String appleClientId, JwtDecoder googleDecoder, JwtDecoder appleDecoder) {
        this("", googleClientId, appleClientId, "", "common", googleDecoder, appleDecoder, appleDecoder);
    }

    OAuthTokenVerifier(
            String googleClientIds,
            String googleClientId,
            String appleClientId,
            JwtDecoder googleDecoder,
            JwtDecoder appleDecoder
    ) {
        this(googleClientIds, googleClientId, appleClientId, "", "common", googleDecoder, appleDecoder, appleDecoder);
    }

    OAuthTokenVerifier(String googleClientIds, String googleClientId, String appleClientId,
            String microsoftClientId, String microsoftTenantId, JwtDecoder googleDecoder,
            JwtDecoder appleDecoder, JwtDecoder microsoftDecoder) {
        this.googleClientIds = normalizeClientIds(googleClientIds, googleClientId);
        this.appleClientId = normalizeClientId(appleClientId);
        this.microsoftClientId = normalizeClientId(microsoftClientId);
        this.microsoftTenantId = normalizeTenantId(microsoftTenantId);
        this.googleDecoder = googleDecoder;
        this.appleDecoder = appleDecoder;
        this.microsoftDecoder = microsoftDecoder;
    }

    public OAuthUserInfo verify(String provider, String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException("OAUTH_TOKEN_REQUIRED", "Token OAuth requis", HttpStatus.BAD_REQUEST);
        }

        String normalizedProvider = provider.toLowerCase(Locale.ROOT);
        requireConfiguredProvider(normalizedProvider);
        Jwt jwt = switch (normalizedProvider) {
            case "google" -> decode(googleDecoder, idToken, "GOOGLE_TOKEN_INVALID", "GOOGLE_TOKEN_EXPIRED");
            case "apple" -> decode(appleDecoder, idToken, "APPLE_TOKEN_INVALID", "APPLE_TOKEN_EXPIRED");
            case "microsoft" -> decode(microsoftDecoder, idToken, "MICROSOFT_TOKEN_INVALID", "MICROSOFT_TOKEN_EXPIRED");
            default -> throw new ApiException("OAUTH_PROVIDER_UNSUPPORTED", "Provider OAuth non supporte", HttpStatus.BAD_REQUEST);
        };

        validateIssuer(normalizedProvider, jwt);
        validateAudience(normalizedProvider, jwt);

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if ("microsoft".equals(normalizedProvider) && (email == null || email.isBlank())) {
            email = jwt.getClaimAsString("preferred_username");
        }
        if (subject == null || subject.isBlank()) {
            throw new ApiException(providerCode(normalizedProvider, "SUBJECT_MISSING"), "Identifiant OAuth introuvable", HttpStatus.UNAUTHORIZED);
        }
        if (!"microsoft".equals(normalizedProvider) && email != null && !email.isBlank() && !emailVerified(jwt)) {
            throw new ApiException(providerCode(normalizedProvider, "EMAIL_NOT_VERIFIED"), "Email OAuth non vérifié", HttpStatus.UNAUTHORIZED);
        }

        return new OAuthUserInfo(normalizedProvider, subject, email);
    }

    private Jwt decode(JwtDecoder decoder, String idToken, String code, String expiredCode) {
        try {
            return decoder.decode(idToken);
        } catch (JwtValidationException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("expired")) {
                throw new ApiException(expiredCode, "Token OAuth expiré", HttpStatus.UNAUTHORIZED);
            }
            throw new ApiException(code, "Token OAuth invalide", HttpStatus.UNAUTHORIZED);
        } catch (JwtException exception) {
            throw new ApiException(code, "Token OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateIssuer(String provider, Jwt jwt) {
        String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
        boolean valid = switch (provider) {
            case "google" -> issuer.equals("https://accounts.google.com") || issuer.equals("accounts.google.com");
            case "apple" -> issuer.equals("https://appleid.apple.com");
            case "microsoft" -> issuer.equals("https://login.microsoftonline.com/" + microsoftTenantId + "/v2.0")
                    || ("common".equals(microsoftTenantId)
                        && issuer.matches("https://login\\.microsoftonline\\.com/[0-9a-fA-F-]+/v2\\.0"));
            default -> false;
        };
        if (!valid) {
            throw new ApiException(providerCode(provider, "ISSUER_INVALID"), "Issuer OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateAudience(String provider, Jwt jwt) {
        Set<String> expectedAudiences = switch (provider) {
            case "google" -> googleClientIds;
            case "apple" -> Set.of(appleClientId);
            case "microsoft" -> Set.of(microsoftClientId);
            default -> Set.of();
        };

        List<String> audiences = jwt.getAudience();
        boolean containsExpectedAudience = audiences.stream().anyMatch(expectedAudiences::contains);
        if (!containsExpectedAudience) {
            throw new ApiException(providerCode(provider, "AUDIENCE_INVALID"), "Audience OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
        if (audiences.size() > 1 && !expectedAudiences.contains(jwt.getClaimAsString("azp"))) {
            throw new ApiException("OAUTH_AUTHORIZED_PARTY_INVALID", "Authorized party OAuth invalide", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean emailVerified(Jwt jwt) {
        Object claim = jwt.getClaims().get("email_verified");
        return Boolean.TRUE.equals(claim) || claim instanceof String value && Boolean.parseBoolean(value);
    }

    private String providerCode(String provider, String suffix) {
        return provider.toUpperCase(Locale.ROOT) + "_" + suffix;
    }

    private void requireConfiguredProvider(String provider) {
        boolean configured = switch (provider) {
            case "google" -> !googleClientIds.isEmpty();
            case "apple" -> !appleClientId.isBlank();
            case "microsoft" -> !microsoftClientId.isBlank();
            default -> throw new ApiException(
                    "OAUTH_PROVIDER_UNSUPPORTED",
                    "Provider OAuth non supporte",
                    HttpStatus.BAD_REQUEST);
        };
        if (!configured) {
            throw new ApiException(
                    "OAUTH_PROVIDER_NOT_CONFIGURED",
                    "Provider OAuth non configure sur cet environnement",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static String normalizeClientId(String value) {
        if (value == null || value.isBlank() || value.startsWith("your-")) {
            return "";
        }
        return value.trim();
    }

    private static Set<String> normalizeClientIds(String values, String legacyValue) {
        Set<String> clientIds = new LinkedHashSet<>();
        Arrays.stream((values == null ? "" : values).split(","))
                .map(OAuthTokenVerifier::normalizeClientId)
                .filter(value -> !value.isBlank())
                .forEach(clientIds::add);

        String legacyClientId = normalizeClientId(legacyValue);
        if (!legacyClientId.isBlank()) clientIds.add(legacyClientId);

        return Set.copyOf(clientIds);
    }

    private static String normalizeTenantId(String value) {
        return value == null || value.isBlank() ? "common" : value.trim();
    }

    public record OAuthUserInfo(String provider, String providerUserId, String email) {
    }
}
