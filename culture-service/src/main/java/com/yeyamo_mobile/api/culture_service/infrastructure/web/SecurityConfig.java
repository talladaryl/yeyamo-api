package com.yeyamo_mobile.api.culture_service.infrastructure.web;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, Authorities converter) throws Exception {
        return http.csrf(value -> value.disable())
                .sessionManagement(value -> value.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v1/admin/culture/**").authenticated()
                        .requestMatchers("/api/v1/culture/contributions/**", "/api/v1/culture/language-progress/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/culture/challenges/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/culture/challenge-submissions/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/culture/**").permitAll()
                        .requestMatchers("/api/v1/culture/language-lessons/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    JwtDecoder decoder(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
    }

    @Bean
    Authorities authorities() { return new Authorities(); }

    static final class Authorities implements Converter<Jwt, AbstractAuthenticationToken> {
        public AbstractAuthenticationToken convert(Jwt jwt) {
            Set<String> values = new HashSet<>();
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) roles.forEach(role -> values.add(role.startsWith("ROLE_") ? role : "ROLE_" + role));
            List<String> scopes = jwt.getClaimAsStringList("scopes");
            if (scopes != null) scopes.forEach(scope -> values.add(scope.startsWith("SCOPE_") ? scope : "SCOPE_" + scope));
            List<GrantedAuthority> authorities = values.stream().map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        }
    }
}
