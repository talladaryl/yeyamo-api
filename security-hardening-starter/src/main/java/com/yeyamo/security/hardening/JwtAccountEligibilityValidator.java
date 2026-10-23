package com.yeyamo.security.hardening;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Fails closed for email JWTs that do not prove email verification.
 *
 * Legacy email JWTs do not carry this claim and are rejected deliberately: the
 * resource server cannot distinguish a legacy verified token from a legacy
 * token issued to a PENDING account without consulting auth-service.
 */
final class JwtAccountEligibilityValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_ACCOUNT = new OAuth2Error(
            "invalid_token", "Account is not eligible for an authenticated session", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()
                && !Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        String status = jwt.getClaimAsString("account_status");
        if (status != null && !"ACTIVE".equals(status)) {
            return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
