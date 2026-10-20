package com.yeyamo_mobile.api.culture_service.application;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MediaAssociationClient {
    public enum Usage { HERO, VIDEO, GALLERY, STEP }
    public record Reference(UUID mediaId, Usage usage) {}
    private record Request(String contributorId, List<Reference> references, boolean publication) {}
    private record Result(UUID mediaId, boolean allowed, String reason, boolean publicationAllowed) {}
    private record Response(List<Result> results) {}
    private final RestClient client; private final JwtEncoder encoder; private final Duration ttl;

    public MediaAssociationClient(RestClient.Builder builder,
            @Value("${yeyamo.media-service.url:http://media-service:8080}") String baseUrl,
            @Value("${yeyamo.service-auth.culture-media.current-secret:}") String secret,
            @Value("${yeyamo.service-auth.culture-media.ttl:PT2M}") Duration ttl) {
        client=builder.baseUrl(baseUrl).build(); this.ttl=ttl.compareTo(Duration.ofMinutes(5))>0?Duration.ofMinutes(5):ttl;
        if(secret==null||secret.isBlank()) encoder=null;
        else { if(secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("Culture/media service JWT secret must contain at least 32 bytes");
            encoder=new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"))); }
    }
    public void validate(String contributorId,Collection<Reference> references,boolean publication){
        if(references.isEmpty())return;if(encoder==null)throw unavailable();
        try{Response response=client.post().uri("/service/v1/media/culture/validate").header("X-Yeyamo-Service-Token",token(contributorId))
            .body(new Request(contributorId,List.copyOf(references),publication)).retrieve().body(Response.class);
            if(response==null||response.results()==null||response.results().size()!=references.size())throw unavailable();
            response.results().stream().filter(r->!r.allowed()||publication&&!r.publicationAllowed()).findFirst().ifPresent(r->{throw new CultureException("MEDIA_ASSOCIATION_REJECTED","Association média refusée: "+r.reason(),HttpStatus.UNPROCESSABLE_ENTITY);});
        }catch(CultureException e){throw e;}catch(RestClientException e){throw unavailable();}
    }
    private String token(String actor){Instant now=Instant.now();JwtClaimsSet claims=JwtClaimsSet.builder().issuer("culture-service").subject("culture-service").audience(List.of("media-service"))
        .issuedAt(now).expiresAt(now.plus(ttl)).id(UUID.randomUUID().toString()).claim("scope",List.of("media:associate:culture")).claim("act",actor).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();}
    private CultureException unavailable(){return new CultureException("MEDIA_VALIDATION_UNAVAILABLE","Le service de validation média est indisponible",HttpStatus.SERVICE_UNAVAILABLE);}
}
