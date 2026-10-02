package com.yeyamo_mobile.api.notification_service.infrastructure.messaging;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ContentOwnerClient {
    private final RestClient client;
    private final String internalToken;
    public ContentOwnerClient(RestClient.Builder builder,
            @Value("${yeyamo.services.content.url:http://content-service:8080}") String baseUrl,
            @Value("${yeyamo.security.internal-token:}") String internalToken) {
        this.client = builder.baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }
    public String postOwner(UUID postId) {
        if (internalToken.isBlank()) throw new IllegalStateException("INTERNAL_SERVICE_TOKEN_REQUIRED");
        var response = client.get().uri("/internal/posts/{id}/owner", postId)
                .header("X-Internal-Token", internalToken).retrieve().body(PostOwner.class);
        if (response == null || response.ownerAuthUserId() == null || response.ownerAuthUserId().isBlank())
            throw new IllegalStateException("POST_OWNER_UNAVAILABLE");
        return response.ownerAuthUserId();
    }
    private record PostOwner(UUID postId, String ownerAuthUserId) {}
}
