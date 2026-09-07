package com.yeyamo_mobile.api.culture_service.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "culture_proverb_details")
public class ProverbDetailsEntity {
    @Id
    @Column(name = "content_id")
    private UUID contentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "content_id")
    private CultureContent content;

    @Column(name = "literal_translation", nullable = false, columnDefinition = "TEXT")
    private String literalTranslation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_language_code", nullable = false, referencedColumnName = "code")
    private Language originLanguage;

    @Column(name = "audio_url", length = 2048)
    private String audioUrl;

    protected ProverbDetailsEntity() { }

    public static ProverbDetailsEntity create(CultureContent content, String literalTranslation, String meaning,
            Language originLanguage, String audioUrl) {
        ProverbDetailsEntity details = new ProverbDetailsEntity();
        details.content = content;
        details.literalTranslation = literalTranslation.trim();
        details.meaning = meaning.trim();
        details.originLanguage = originLanguage;
        details.audioUrl = blankToNull(audioUrl);
        return details;
    }

    public UUID getContentId() { return contentId; }
    public String getLiteralTranslation() { return literalTranslation; }
    public String getMeaning() { return meaning; }
    public Language getOriginLanguage() { return originLanguage; }
    public String getAudioUrl() { return audioUrl; }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
