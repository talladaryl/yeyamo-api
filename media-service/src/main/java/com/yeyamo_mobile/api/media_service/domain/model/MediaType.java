package com.yeyamo_mobile.api.media_service.domain.model;

public enum MediaType {
    IMAGE,
    VIDEO,
    /** Pronunciation, lesson audio, oral history, artisan story audio. */
    AUDIO,
    /** Historical documents, PDFs, text files. */
    DOCUMENT,
    /** Authenticity certificates (more restricted than DOCUMENT). */
    CERTIFICATE
}
