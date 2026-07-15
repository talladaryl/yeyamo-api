package com.yeyamo.security.hardening;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

public final class StrictJwtDecoders {
    private StrictJwtDecoders() {
    }

    public static JwtDecoder create(String secret, String previousSecrets, String jwkSetUri,
            String issuer, String audience) {
        OAuth2TokenValidator<Jwt> validator = validator(issuer, audience);
        if (hasText(jwkSetUri)) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri.trim()).build();
            decoder.setJwtValidator(validator);
            return decoder;
        }
        List<String> secrets = new ArrayList<>();
        secrets.add(requiredSecret(secret));
        if (hasText(previousSecrets)) {
            Arrays.stream(previousSecrets.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(StrictJwtDecoders::requiredSecret)
                    .forEach(secrets::add);
        }
        List<JwtDecoder> decoders = secrets.stream()
                .map(value -> (JwtDecoder) hmac(value, validator))
                .toList();
        return token -> decodeWithRotation(token, decoders);
    }

    private static NimbusJwtDecoder hmac(String secret, OAuth2TokenValidator<Jwt> validator) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private static Jwt decodeWithRotation(String token, List<JwtDecoder> decoders) {
        JwtException last = null;
        for (JwtDecoder decoder : decoders) {
            try {
                return decoder.decode(token);
            } catch (JwtException exception) {
                last = exception;
            }
        }
        throw last == null ? new JwtException("JWT validation failed") : last;
    }

    private static OAuth2TokenValidator<Jwt> validator(String issuer, String audience) {
        if (!hasText(issuer) || !hasText(audience)) {
            throw new IllegalArgumentException("JWT issuer and audience are required");
        }
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer.trim());
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience.trim())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "Required JWT audience is missing", null));
        return new DelegatingOAuth2TokenValidator<>(timestamps, issuerValidator, audienceValidator);
    }

    private static String requiredSecret(String secret) {
        if (!hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secrets must contain at least 32 bytes");
        }
        return secret;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
