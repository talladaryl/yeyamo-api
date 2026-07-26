package com.yeyamo_mobile.api.commerce_service.config;
import org.springframework.context.annotation.*;import org.springframework.security.config.annotation.web.builders.*;import org.springframework.security.web.*;
@Configuration public class SecurityConfig{@Bean SecurityFilterChain chain(HttpSecurity h)throws Exception{return h.csrf(c->c.disable()).authorizeHttpRequests(a->a.requestMatchers("/actuator/health").permitAll().requestMatchers("/api/v1/commerce/admin/**").hasRole("ADMIN").anyRequest().authenticated()).oauth2ResourceServer(o->o.jwt(j->{})).build();}}
