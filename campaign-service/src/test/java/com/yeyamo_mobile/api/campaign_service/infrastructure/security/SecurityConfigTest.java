package com.yeyamo_mobile.api.campaign_service.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {
    @Test
    void grantsOnlyAuthoritiesPresentInVerifiedJwtClaims() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of(
                        "sub", "admin-7",
                        "roles", List.of("ADMIN"),
                        "scope", "campaign:approve",
                        "permissions", List.of("campaign:audit")));
        assertThat(new SecurityConfig().authorities(jwt)).extracting("authority").containsExactlyInAnyOrder(
                "ROLE_ADMIN", "SCOPE_campaign:approve", "PERMISSION_campaign:audit");
    }

    @Test
    void missingScopeNeverCreatesApprovalAuthority() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", "admin-7", "roles", List.of("ADMIN")));
        assertThat(new SecurityConfig().authorities(jwt)).extracting("authority")
                .doesNotContain("SCOPE_campaign:approve", "SCOPE_campaign:reject");
    }
}
