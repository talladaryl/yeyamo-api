package com.yeyamo_mobile.api.catalog_service.infrastructure.security;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a->a.requestMatchers("/actuator/health/**","/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/catalog/assets/**").permitAll()
                        .requestMatchers("/api/v1/catalog/assets/**").hasAnyRole("ADMIN","PARTNER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o->o.jwt(j->{})).build();
    }
    @Bean JwtDecoder decoder(@Value("${jwt.secret}")String secret){
        if(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        SecretKey key=new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}
