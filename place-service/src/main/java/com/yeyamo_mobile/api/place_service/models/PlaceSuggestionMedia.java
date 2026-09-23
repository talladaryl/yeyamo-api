package com.yeyamo_mobile.api.place_service.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Immutable media reference captured when a pending suggestion is submitted. */
@Entity
@Table(name = "place_suggestion_media")
public class PlaceSuggestionMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggestion_id", nullable = false)
    private PlaceSuggestion suggestion;

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlaceSuggestionMedia() { }

    public static PlaceSuggestionMedia from(PlaceSuggestion suggestion, UUID mediaId, String type, String contentType,
            String contentUrl, String thumbnailUrl, int displayOrder) {
        PlaceSuggestionMedia value = new PlaceSuggestionMedia();
        value.suggestion = suggestion;
        value.mediaId = mediaId;
        value.type = type;
        value.contentType = contentType;
        value.contentUrl = contentUrl;
        value.thumbnailUrl = thumbnailUrl;
        value.displayOrder = displayOrder;
        value.createdAt = Instant.now();
        return value;
    }

    public Long getId() { return id; }
    public UUID getMediaId() { return mediaId; }
    public String getType() { return type; }
    public String getContentType() { return contentType; }
    public String getContentUrl() { return contentUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public int getDisplayOrder() { return displayOrder; }
}
