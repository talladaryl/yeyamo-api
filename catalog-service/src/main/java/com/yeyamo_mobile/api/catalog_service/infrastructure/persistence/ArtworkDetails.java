package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

public final class ArtworkDetails { private ArtworkDetails() {}
    public enum TranslationStatus { DRAFT, REVIEWED, VERIFIED, REJECTED }
    public enum VerificationStatus { DECLARED, REVIEWED, VERIFIED, DISPUTED }
    public enum MediaType { PRIMARY_IMAGE, GALLERY_IMAGE, VIDEO, CREATION_PROCESS, ARTISAN_AUDIO, HISTORY_AUDIO, CERTIFICATE }
    @Entity @Table(name="artwork_translations") public static class Translation {
        @Id public UUID id; @Column(name="artwork_id") public UUID artworkId; @Column(name="language_code") public String languageCode;
        public String title; @Column(name="short_description") public String shortDescription; public String story;
        @Enumerated(EnumType.STRING) public TranslationStatus status; @Column(name="translator_id") public String translatorId;
    }
    @Entity @Table(name="artwork_history_entries") public static class History {
        @Id public UUID id; @Column(name="artwork_id") public UUID artworkId; public String title; public String narrative;
        @Column(name="language_code") public String languageCode; public String period; @Column(name="cultural_meaning") public String culturalMeaning;
        public String source; @Column(name="contributor_id") public String contributorId; @Enumerated(EnumType.STRING) @Column(name="verification_status") public VerificationStatus verificationStatus;
        @Column(name="created_at") public Instant createdAt; @Column(name="updated_at") public Instant updatedAt; @Version public long version;
        @PrePersist void create(){createdAt=updatedAt=Instant.now();} @PreUpdate void update(){updatedAt=Instant.now();}
    }
    @Entity @Table(name="artwork_media") @IdClass(MediaId.class) public static class Media {
        @Id @Column(name="artwork_id") public UUID artworkId; @Id @Column(name="media_id") public UUID mediaId;
        @Enumerated(EnumType.STRING) @Column(name="media_type") public MediaType mediaType; @Column(name="display_order") public int displayOrder;
    }
    public static class MediaId implements java.io.Serializable { public UUID artworkId; public UUID mediaId; public MediaId(){} public MediaId(UUID a,UUID m){artworkId=a;mediaId=m;} }
}
