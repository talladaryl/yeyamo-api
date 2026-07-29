package com.yeyamo_mobile.api.api_gateway.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class SecurityConfig {
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/oauth/google",
            "/api/v1/auth/oauth/apple",
            "/api/v1/auth/email/verification/request",
            "/api/v1/auth/email/verification/confirm",
            "/api/v1/auth/password/forgot",
            "/api/v1/auth/password/reset");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/fallback/**",
                                "/openapi/**", "/mobile-api/**").permitAll()
                        .requestMatchers(SecurityConfig::isPublicMobileRequest).permitAll()
                        .requestMatchers("/api/v1/admin/campaigns/**").authenticated()
                        .requestMatchers("/api/v1/admin/platform-users/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "SUPPORT")
                        .requestMatchers("/api/v1/admin/**", "/api/v1/analytics/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "MODERATOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(authoritiesConverter())))
                .build();
    }

    static boolean isPublicMobileRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (PUBLIC_AUTH_PATHS.contains(path)) {
            return true;
        }
        if ("POST".equals(method) && path.startsWith("/api/v1/payments/webhooks/")) {
            return true;
        }
        if (!"GET".equals(method)) {
            return false;
        }
        if ("/api/v1/events/me".equals(path)) {
            return false;
        }
        return path.matches("^/api/v1/(places|regions|cities|districts|categories|events|media)(/.*)?$")
                || path.matches("^/api/v1/catalog/(assets|regions|cities|categories)(/.*)?$")
                || path.matches("^/api/v1/posts/(hashtags|catalog)/.*$")
                || path.matches("^/api/v1/interactions/posts/.*$")
                || path.matches("^/api/v1/posts/[0-9a-fA-F-]{36}$")
                || path.matches("^/api/v1/users(?:/[0-9a-fA-F-]{36})?$")
                || path.matches("^/api/v1/partners(?:/[0-9a-fA-F-]{36})?$");
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> authoritiesConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = new ArrayList<>();
        List<String> roleClaims = jwt.getClaimAsStringList("roles");
        if (roleClaims != null) {
            roles.addAll(roleClaims);
        }
        String singleRole = jwt.getClaimAsString("role");
        if (singleRole != null && !singleRole.isBlank()) {
            roles.add(singleRole);
        }
        List<GrantedAuthority> authorities = new ArrayList<>(roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList());
        addAuthorities(authorities, jwt.getClaimAsStringList("scopes"), "SCOPE_");
        addAuthorities(authorities, splitClaim(jwt.getClaimAsString("scope")), "SCOPE_");
        addAuthorities(authorities, jwt.getClaimAsStringList("permissions"), "PERMISSION_");
        return authorities.stream().distinct().toList();
    }

    private void addAuthorities(List<GrantedAuthority> authorities, List<String> values, String prefix) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.startsWith(prefix) ? value : prefix + value)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }

    private List<String> splitClaim(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.trim().split("\\s+"));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${security.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID",
                "Idempotency-Key", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("X-Correlation-ID", "X-RateLimit-Limit",
                "X-RateLimit-Remaining", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
