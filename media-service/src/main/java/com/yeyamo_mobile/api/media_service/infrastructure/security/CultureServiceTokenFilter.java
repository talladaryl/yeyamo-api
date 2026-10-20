package com.yeyamo_mobile.api.media_service.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates only the private Culture -> Media contract. User JWTs are never accepted here. */
@Component
public final class CultureServiceTokenFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Yeyamo-Service-Token";
    private static final String PATH = "/service/v1/media/culture/validate";
    private final List<JwtDecoder> decoders;
    private final ObjectMapper mapper;

    public CultureServiceTokenFilter(
            @Value("${yeyamo.service-auth.culture-media.current-secret:}") String current,
            @Value("${yeyamo.service-auth.culture-media.previous-secret:}") String previous,
            ObjectMapper mapper) {
        this.mapper = mapper;
        this.decoders = new ArrayList<>();
        addDecoder(current);
        addDecoder(previous);
    }

    private void addDecoder(String value) {
        if (value == null || value.isBlank()) return;
        if (value.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Culture/media service JWT secret must contain at least 32 bytes");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer("culture-service");
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains("media-service")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid service-token audience", null));
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ofSeconds(30));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience, timestamp));
        decoders.add(decoder);
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !PATH.equals(request.getRequestURI()); }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String raw = request.getHeader(HEADER);
        Jwt jwt = decode(raw);
        List<String> scopes = jwt == null ? null : jwt.getClaimAsStringList("scope");
        if (jwt == null || !"culture-service".equals(jwt.getSubject()) || scopes == null ||
                !scopes.contains("media:associate:culture")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), java.util.Map.of("code", "SERVICE_AUTHENTICATION_FAILED", "message", "Service authentication failed"));
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(jwt, raw,
                List.of(new SimpleGrantedAuthority("SCOPE_media:associate:culture")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try { chain.doFilter(request, response); } finally { SecurityContextHolder.clearContext(); }
    }

    private Jwt decode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (JwtDecoder decoder : decoders) try { return decoder.decode(raw); } catch (JwtException ignored) { }
        return null;
    }
}
