package com.yeyamo_mobile.api.feed_service.infrastructure.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.yeyamo_mobile.api.feed_service.application.port.FollowingAuthorResolver;

/** Propagates the viewer's bearer token to user-service; an unavailable social
 * graph is an error, never an apparently valid empty Following feed. */
@Component
public class UserServiceFollowingAuthorClient implements FollowingAuthorResolver {
    private final RestClient client;

    public UserServiceFollowingAuthorClient(
            @Value("${yeyamo.user-service.base-url:http://localhost:8086}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public List<String> followedAuthorIds(String viewerAuthUserId, String bearerToken, String correlationId) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La source des abonnements est indisponible");
        }
        try {
            List<String> ids = client.get()
                    .uri("/api/v1/users/social/following/content-author-ids")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .header("X-Correlation-ID", correlationId == null ? "" : correlationId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() { });
            return ids == null ? List.of() : ids;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La source des abonnements est indisponible", exception);
        }
    }
}
