package com.yeyamo_mobile.api.media_service.infrastructure.security;
import java.nio.charset.StandardCharsets;import java.util.*;import javax.crypto.*;import javax.crypto.spec.SecretKeySpec;import org.springframework.beans.factory.annotation.Value;import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;import org.springframework.http.HttpMethod;import org.springframework.security.authentication.AbstractAuthenticationToken;import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;import org.springframework.security.core.*;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig{
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .authorizeHttpRequests(a->a.requestMatchers("/actuator/health/**","/actuator/info","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
   .requestMatchers(HttpMethod.GET,"/api/v1/media/**").permitAll().requestMatchers("/api/v1/media/**").authenticated().anyRequest().authenticated())
  .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(authorities()))).build();}
 @Bean JwtDecoder decoder(@Value("${jwt.secret}")String secret){if(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
  SecretKey key=new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");return NimbusJwtDecoder.withSecretKey(key).build();}
 @Bean JwtRolesConverter authorities(){return new JwtRolesConverter();}
 static final class JwtRolesConverter implements Converter<Jwt,AbstractAuthenticationToken>{
  @Override public AbstractAuthenticationToken convert(Jwt jwt){List<String> roles=new ArrayList<>();List<String> list=jwt.getClaimAsStringList("roles");if(list!=null)roles.addAll(list);
  String role=jwt.getClaimAsString("role");if(role!=null&&!role.isBlank())roles.add(role);Collection<GrantedAuthority> auth=roles.stream().map(r->r.startsWith("ROLE_")?r:"ROLE_"+r).distinct()
   .map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();return new JwtAuthenticationToken(jwt,auth,jwt.getSubject());}
 }
}
