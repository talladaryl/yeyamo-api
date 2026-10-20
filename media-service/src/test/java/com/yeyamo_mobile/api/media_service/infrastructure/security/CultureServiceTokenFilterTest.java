package com.yeyamo_mobile.api.media_service.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

class CultureServiceTokenFilterTest {
    private static final String SECRET="culture-media-test-secret-which-is-long-enough-123";
    @Test void acceptsValidScopedToken() throws Exception { assertEquals(200,run(token("media-service",List.of("media:associate:culture"),Instant.now().plusSeconds(60)))); }
    @Test void rejectsMissingToken() throws Exception { assertEquals(401,run(null)); }
    @Test void rejectsWrongAudience() throws Exception { assertEquals(401,run(token("other-service",List.of("media:associate:culture"),Instant.now().plusSeconds(60)))); }
    @Test void rejectsMissingScope() throws Exception { assertEquals(401,run(token("media-service",List.of("other"),Instant.now().plusSeconds(60)))); }
    @Test void rejectsExpiredToken() throws Exception { assertEquals(401,run(token("media-service",List.of("media:associate:culture"),Instant.now().minusSeconds(90)))); }
    private int run(String token)throws Exception{var filter=new CultureServiceTokenFilter(SECRET,"",new ObjectMapper());var request=new MockHttpServletRequest("POST","/service/v1/media/culture/validate");if(token!=null)request.addHeader(CultureServiceTokenFilter.HEADER,token);var response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());return response.getStatus();}
    private String token(String audience,List<String> scopes,Instant expires){var encoder=new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256")));Instant now=Instant.now();var claims=JwtClaimsSet.builder().issuer("culture-service").subject("culture-service").audience(List.of(audience)).issuedAt(now.minusSeconds(120)).expiresAt(expires).claim("scope",scopes).claim("act","u").build();return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();}
}
