package com.yeyamo_mobile.api.graph_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.yeyamo.security.hardening.StrictJwtDecoders;

@Configuration
@EnableMethodSecurity
public class GraphSecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/culture-graph/**")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/culture-graph/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtDecoder decoder(
            @org.springframework.beans.factory.annotation.Value("${jwt.secret}") String secret,
            @org.springframework.beans.factory.annotation.Value("${jwt.previous-secrets:}") String previousSecrets,
            @org.springframework.beans.factory.annotation.Value("${jwt.jwk-set-uri:}") String jwkSetUri,
            @org.springframework.beans.factory.annotation.Value("${jwt.issuer:yeyamo-auth}") String issuer,
            @org.springframework.beans.factory.annotation.Value("${jwt.audience:yeyamo-api}") String audience) {
        return StrictJwtDecoders.create(secret, previousSecrets, jwkSetUri, issuer, audience);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var roles = jwt.getClaimAsStringList("roles");
            return roles == null
                    ? java.util.List.of()
                    : roles.stream()
                            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                                    role.startsWith("ROLE_") ? role : "ROLE_" + role))
                            .toList();
        });
        return converter;
    }
}
