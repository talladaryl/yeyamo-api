package com.yeyamo_mobile.api.media_service.domain.model;

/**
 * Controlled vocabulary for the intended usage of an uploaded media asset.
 *
 * <p>Each value is associated with a permitted {@link MediaType} via
 * {@link #allowedType()}.  The upload API validates that the supplied
 * {@code usageType} is consistent with the declared content-type so no
 * arbitrary string can slip through.</p>
 */
public enum MediaUsageType {

    // ---- Images -----------------------------------------------------------
    ARTWORK_PRIMARY_IMAGE(MediaType.IMAGE),
    ARTWORK_GALLERY(MediaType.IMAGE),
    ARTWORK_CREATION_PROCESS(MediaType.IMAGE),
    PROFILE_PHOTO(MediaType.IMAGE),
    POST_IMAGE(MediaType.IMAGE),

    // ---- Audio ------------------------------------------------------------
    ARTISAN_STORY_AUDIO(MediaType.AUDIO),
    CULTURE_STORY_AUDIO(MediaType.AUDIO),
    LANGUAGE_PRONUNCIATION(MediaType.AUDIO),
    LESSON_AUDIO(MediaType.AUDIO),

    // ---- Documents --------------------------------------------------------
    HISTORICAL_DOCUMENT(MediaType.DOCUMENT),

    // ---- Certificates -----------------------------------------------------
    CERTIFICATE_DOCUMENT(MediaType.CERTIFICATE),

    // ---- Video ------------------------------------------------------------
    POST_VIDEO(MediaType.VIDEO),
    ARTWORK_VIDEO(MediaType.VIDEO);

    private final MediaType allowedType;

    MediaUsageType(MediaType allowedType) { this.allowedType = allowedType; }

    /** The only {@link MediaType} that is compatible with this usage. */
    public MediaType allowedType() { return allowedType; }
}
