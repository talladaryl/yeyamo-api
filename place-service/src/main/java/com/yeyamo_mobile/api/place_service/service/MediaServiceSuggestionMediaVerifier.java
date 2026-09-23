package com.yeyamo_mobile.api.place_service.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.place_service.exception.ApiException;

/** Uses the existing public metadata contract exposed by media-service. */
@Component
public class MediaServiceSuggestionMediaVerifier implements SuggestionMediaVerifier {
    private final RestClient mediaClient;

    public MediaServiceSuggestionMediaVerifier(RestClient.Builder builder,
            @Value("${yeyamo.services.media.url:http://media-service}") String serviceUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        mediaClient = builder.baseUrl(serviceUrl.replaceAll("/$", "")).requestFactory(factory).build();
    }

    @Override
    public List<VerifiedMedia> verifyOwnedUsableVisualMedia(List<UUID> mediaIds, String ownerId) {
        if (mediaIds == null || mediaIds.isEmpty()) return List.of();
        List<VerifiedMedia> verified = new ArrayList<>(mediaIds.size());
        for (UUID mediaId : mediaIds) {
            Map<String, Object> media = metadata(mediaId);
            if (!ownerId.equals(media.get("ownerId"))) {
                throw new ApiException("MEDIA_NOT_OWNED", "Ce media ne vous appartient pas", HttpStatus.FORBIDDEN);
            }
            String status = string(media, "status");
            if (!"READY".equals(status)) {
                throw new ApiException("MEDIA_NOT_USABLE", "Ce media n'est pas pret a etre utilise", HttpStatus.CONFLICT);
            }
            String type = string(media, "type");
            if (!"IMAGE".equals(type) && !"VIDEO".equals(type)) {
                throw new ApiException("MEDIA_NOT_USABLE", "Seuls les images et videos sont acceptes", HttpStatus.BAD_REQUEST);
            }
            verified.add(new VerifiedMedia(mediaId, type, string(media, "contentType"),
                    string(media, "contentUrl"), string(media, "thumbnailUrl")));
        }
        return List.copyOf(verified);
    }

    private Map<String, Object> metadata(UUID mediaId) {
        try {
            Map<String, Object> response = mediaClient.get().uri("/api/v1/media/{id}", mediaId).retrieve()
                    .onStatus(status -> status.value() == 404, (request, responseError) -> {
                        throw new ApiException("MEDIA_NOT_FOUND", "Media introuvable", HttpStatus.NOT_FOUND);
                    })
                    .onStatus(HttpStatusCode::isError, (request, responseError) -> {
                        throw new ApiException("MEDIA_NOT_USABLE", "Media indisponible", HttpStatus.CONFLICT);
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            if (response == null) {
                throw new ApiException("MEDIA_NOT_FOUND", "Media introuvable", HttpStatus.NOT_FOUND);
            }
            return response;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException("MEDIA_VALIDATION_UNAVAILABLE", "Validation media temporairement indisponible",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String string(Map<String, Object> media, String field) {
        Object value = media.get(field);
        return value == null ? null : value.toString();
    }
}
