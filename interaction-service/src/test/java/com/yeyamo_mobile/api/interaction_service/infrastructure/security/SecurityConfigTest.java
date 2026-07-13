package com.yeyamo_mobile.api.interaction_service.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

    @Test
    void convertsSingleAndMultipleJwtRoles() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"),
                Map.of("sub", "user-42", "role", "ADMIN", "roles", List.of("USER", "ROLE_MODERATOR")));

        var authentication = new SecurityConfig.JwtRolesConverter().convert(jwt);

        assertNotNull(authentication);
        assertEquals("user-42", authentication.getName());
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR")));
    }
}
