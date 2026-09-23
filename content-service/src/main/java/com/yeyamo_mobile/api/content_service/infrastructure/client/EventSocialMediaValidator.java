package com.yeyamo_mobile.api.content_service.infrastructure.client;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.yeyamo_mobile.api.content_service.application.ContentException;

/**
 * Reuses public media metadata instead of trusting an Event cover ID blindly.
 * Only ready IMAGE/VIDEO media owned by the Event organizer may be attached to
 * automatically generated social content.
 */
@Component
public class EventSocialMediaValidator {
    private final RestClient rest;
    private final String mediaService;

    public EventSocialMediaValidator(
            RestClient.Builder builder,
            @Value("${yeyamo.services.media:http://media-service:8101}") String mediaService) {
        this.rest = builder.build();
        this.mediaService = mediaService;
    }

    public MediaEligibility inspect(UUID mediaId, String expectedOwnerId) {
        if (mediaId == null) {
            return MediaEligibility.unavailableForStory();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = rest.get()
                    .uri(mediaService + "/api/v1/media/" + mediaId)
                    .retrieve()
                    .body(Map.class);
            if (metadata == null
                    || !expectedOwnerId.equals(text(metadata, "ownerId"))
                    || !"READY".equals(text(metadata, "status"))
                    || !("IMAGE".equals(text(metadata, "type")) || "VIDEO".equals(text(metadata, "type")))
                    || !validMime(text(metadata, "contentType"))) {
                return MediaEligibility.unavailableForStory();
            }
            return MediaEligibility.usable(mediaId);
        } catch (RestClientResponseException exception) {
            // A missing or inaccessible cover is final: do not fabricate an
            // asset. Server-side media failures remain retryable.
            if (exception.getStatusCode().is4xxClientError()) {
                return MediaEligibility.unavailableForStory();
            }
            throw new ContentException("EVENT_SOCIAL_MEDIA_UNAVAILABLE", "La verification du media de sortie est temporairement indisponible");
        } catch (ResourceAccessException exception) {
            throw new ContentException("EVENT_SOCIAL_MEDIA_UNAVAILABLE", "La verification du media de sortie est temporairement indisponible");
        } catch (RestClientException exception) {
            throw new ContentException("EVENT_SOCIAL_MEDIA_UNAVAILABLE", "La verification du media de sortie est temporairement indisponible");
        }
    }

    private boolean validMime(String contentType) {
        return contentType != null
                && (contentType.regionMatches(true, 0, "image/", 0, 6)
                || contentType.regionMatches(true, 0, "video/", 0, 6));
    }

    private String text(Map<String, Object> metadata, String field) {
        Object value = metadata.get(field);
        return value == null ? null : value.toString();
    }

    public record MediaEligibility(UUID mediaId, boolean usableForSocialContent) {
        public static MediaEligibility usable(UUID mediaId) { return new MediaEligibility(mediaId, true); }
        public static MediaEligibility unavailableForStory() { return new MediaEligibility(null, false); }
    }
}
