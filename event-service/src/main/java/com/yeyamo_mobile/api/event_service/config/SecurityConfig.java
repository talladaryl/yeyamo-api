package com.yeyamo_mobile.api.event_service.config;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http,JwtRolesConverter converter)throws Exception{
        return http.csrf(csrf->csrf.disable())
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth->auth
                    .requestMatchers("/actuator/health/**","/actuator/info","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
                    .requestMatchers(HttpMethod.GET,"/api/v1/events/me").authenticated()
                    .requestMatchers(HttpMethod.GET,"/api/v1/events/**","/api/v1/places/*/events").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/v1/events/*/register").authenticated()
                    .requestMatchers(HttpMethod.DELETE,"/api/v1/events/*/unregister").authenticated()
                    .requestMatchers("/api/v1/events/**").hasAnyRole("ADMIN","SUPER_ADMIN","PARTNER")
                    .anyRequest().authenticated())
                .oauth2ResourceServer(oauth->oauth.jwt(jwt->jwt.jwtAuthenticationConverter(converter))).build();
    }
    @Bean JwtDecoder decoder(@Value("${jwt.secret}")String secret){
        if(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32)
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        SecretKey key=new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
    @Bean JwtRolesConverter rolesConverter(){return new JwtRolesConverter();}
    static final class JwtRolesConverter implements Converter<Jwt,AbstractAuthenticationToken>{
        public AbstractAuthenticationToken convert(Jwt jwt){
            List<String> roles=new ArrayList<>();
            List<String> values=jwt.getClaimAsStringList("roles");if(values!=null)roles.addAll(values);
            String role=jwt.getClaimAsString("role");if(role!=null&&!role.isBlank())roles.add(role);
            Collection<GrantedAuthority> authorities=roles.stream().map(v->v.startsWith("ROLE_")?v:"ROLE_"+v)
                    .distinct().map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
            return new JwtAuthenticationToken(jwt,authorities,jwt.getSubject());
        }
    }
}
