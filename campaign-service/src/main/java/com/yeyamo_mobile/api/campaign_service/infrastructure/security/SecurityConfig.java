package com.yeyamo_mobile.api.campaign_service.infrastructure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import com.yeyamo.security.hardening.StrictJwtDecoders;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> campaignJwtAuthenticationConverter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(campaignJwtAuthenticationConverter)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(org.springframework.core.env.Environment environment) {
        return StrictJwtDecoders.create(
                environment.getProperty("jwt.secret"),
                environment.getProperty("jwt.previous-secrets", ""),
                environment.getProperty("jwt.jwk-set-uri", ""),
                environment.getProperty("jwt.issuer", "https://auth.yeyamo.internal"),
                environment.getProperty("jwt.audience", "yeyamo-api"));
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> campaignJwtAuthenticationConverter() {
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            @Override
            public AbstractAuthenticationToken convert(Jwt jwt) {
                return new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
            }
        };
    }

    Collection<GrantedAuthority> authorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        add(authorities, jwt.getClaimAsStringList("roles"), "ROLE_");
        add(authorities, jwt.getClaimAsStringList("scopes"), "SCOPE_");
        add(authorities, split(jwt.getClaimAsString("scope")), "SCOPE_");
        add(authorities, jwt.getClaimAsStringList("permissions"), "PERMISSION_");
        return authorities.stream().distinct().toList();
    }

    private void add(List<GrantedAuthority> authorities, List<String> values, String prefix) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.startsWith(prefix) ? value : prefix + value)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }

    private List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.trim().split("\\s+"));
    }
}
