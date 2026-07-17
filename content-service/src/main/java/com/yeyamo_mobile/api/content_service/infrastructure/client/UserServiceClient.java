package com.yeyamo_mobile.api.content_service.infrastructure.client;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client HTTP pour communiquer avec user-service.
 * Utilisé pour récupérer la liste des comptes suivis (following).
 */
@Component
public class UserServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${yeyamo.user-service.base-url:http://localhost:8086}") String userServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    /**
     * Récupère les IDs des utilisateurs suivis par l'utilisateur donné.
     * Appelle GET /api/v1/users/social/following de user-service.
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des IDs des comptes suivis (vide si erreur)
     */
    public List<String> getFollowingIds(String userId) {
        try {
            String url = userServiceBaseUrl + "/api/v1/users/social/following?page=0&size=1000";
            
            // Note: En production, il faudrait propager le JWT Bearer token
            // Pour l'instant, on suppose que user-service accepte les appels internes
            
            ResponseEntity<PageResponse<UserProfileSummary>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PageResponse<UserProfileSummary>>() {}
            );
            
            if (response.getBody() != null && response.getBody().content != null) {
                return response.getBody().content.stream()
                        .map(profile -> profile.id.toString())
                        .toList();
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.warn("Failed to fetch following from user-service for user {}: {}", userId, e.getMessage());
            // En cas d'erreur, retourner une liste vide plutôt que de faire échouer la requête
            return List.of();
        }
    }

    // DTOs internes pour le mapping de la réponse
    private static class PageResponse<T> {
        public List<T> content;
        public int totalElements;
    }

    private static class UserProfileSummary {
        public UUID id;
        public String displayName;
    }
}
