package com.yeyamo_mobile.api.place_service.service;

import java.util.List;
import java.util.UUID;

/**
 * Boundary to media-service for place-suggestion attachments. The place bounded
 * context only stores these references; it never stores media bytes.
 */
public interface SuggestionMediaVerifier {
    List<VerifiedMedia> verifyOwnedUsableVisualMedia(List<UUID> mediaIds, String ownerId);

    record VerifiedMedia(UUID mediaId, String type, String contentType, String contentUrl,
            String thumbnailUrl) { }
}
