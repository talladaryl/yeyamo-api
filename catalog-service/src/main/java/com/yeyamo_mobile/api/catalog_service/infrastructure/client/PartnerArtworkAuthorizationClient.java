package com.yeyamo_mobile.api.catalog_service.infrastructure.client;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.yeyamo_mobile.api.catalog_service.application.port.PartnerArtworkAuthorizationPort;
@Component
public class PartnerArtworkAuthorizationClient implements PartnerArtworkAuthorizationPort {
    private final RestClient client; private final String token;
    public PartnerArtworkAuthorizationClient(RestClient.Builder builder,@Value("${yeyamo.services.partner.url:http://partner-service:8101}")String url,@Value("${yeyamo.security.internal-token:${INTERNAL_SERVICE_TOKEN:}}")String token){this.client=builder.baseUrl(url).build();this.token=token;}
    public void requireCanManage(String userId,UUID partnerId){try{Authorization response=client.get().uri("/internal/partners/{partnerId}/users/{userId}/artwork-management",partnerId,userId).header("X-Internal-Token",token).retrieve().onStatus(HttpStatusCode::isError,(request,value)->{throw new SecurityException("Partner artwork authorization denied");}).body(Authorization.class);if(response==null||!response.allowed()||!partnerId.equals(response.partnerId()))throw new SecurityException("Partner artwork authorization denied");}catch(SecurityException exception){throw exception;}catch(Exception exception){throw new SecurityException("Partner artwork authorization unavailable");}}
    public Set<UUID> manageablePartners(String userId){try{UUID[] ids=client.get().uri("/internal/partners/users/{userId}/artwork-management",userId).header("X-Internal-Token",token).retrieve().onStatus(HttpStatusCode::isError,(request,value)->{throw new SecurityException("Partner artwork authorization denied");}).body(UUID[].class);return ids==null?Set.of():Set.of(ids);}catch(SecurityException exception){throw exception;}catch(Exception exception){throw new SecurityException("Partner artwork authorization unavailable");}}
    record Authorization(UUID partnerId,boolean allowed){}
}
