package com.yeyamo_mobile.api.catalog_service.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;import java.util.*;
import org.junit.jupiter.api.Test;import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTests {
    @Test void mapsRoleClaimsToSpringAuthorities(){
        Jwt jwt=new Jwt("token",Instant.now(),Instant.now().plusSeconds(60),Map.of("alg","HS256"),Map.of("sub","partner-1","role","PARTNER","roles",List.of("USER")));
        var auth=new SecurityConfig.JwtRolesConverter().convert(jwt);
        assertEquals("partner-1",auth.getName());assertTrue(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_PARTNER")));
    }
}
