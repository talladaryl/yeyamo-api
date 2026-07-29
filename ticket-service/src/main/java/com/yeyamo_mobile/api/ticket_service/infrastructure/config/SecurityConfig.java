package com.yeyamo_mobile.api.ticket_service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                oauth2.jwt(jwt -> {})
            );
        
        return http.build();
    }
}
