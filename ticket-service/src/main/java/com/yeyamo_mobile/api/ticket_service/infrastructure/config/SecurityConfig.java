package com.yeyamo_mobile.api.ticket_service.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthoritiesConverter authorities) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tickets/events/*/types").permitAll()
                .requestMatchers("/api/v1/admin/events/**").hasAnyRole("ADMIN","SUPER_ADMIN")
                
                // User ticket operations
                .requestMatchers(HttpMethod.POST, "/api/v1/tickets/hold").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/tickets/orders").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/tickets/my-**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/tickets/*/qr").authenticated()
                
                // Scan operations (staff only)
                .requestMatchers(HttpMethod.POST, "/api/v1/tickets/scan").hasAuthority("SCOPE_ticket:scan")
                .requestMatchers(HttpMethod.GET, "/api/v1/tickets/scans/**").hasAuthority("SCOPE_ticket:scan")
                
                // Partner operations
                .requestMatchers("/api/v1/tickets/partner/**").hasAuthority("SCOPE_partner")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(authorities))
            );
        
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        return NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
    }

    @Bean
    JwtAuthoritiesConverter jwtAuthoritiesConverter() {
        return new JwtAuthoritiesConverter();
    }

    static final class JwtAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            add(jwt.getClaimAsStringList("roles"), "ROLE_", authorities);
            add(jwt.getClaimAsStringList("scopes"), "SCOPE_", authorities);
            add(jwt.getClaimAsStringList("permissions"), "", authorities);
            return new JwtAuthenticationToken(jwt, authorities.stream().distinct().toList(), jwt.getSubject());
        }

        private void add(Collection<String> values, String prefix, List<GrantedAuthority> authorities) {
            if (values == null) return;
            values.stream().filter(value -> value != null && !value.isBlank())
                    .map(value -> value.startsWith(prefix) ? value : prefix + value)
                    .map(SimpleGrantedAuthority::new).forEach(authorities::add);
        }
    }
}
