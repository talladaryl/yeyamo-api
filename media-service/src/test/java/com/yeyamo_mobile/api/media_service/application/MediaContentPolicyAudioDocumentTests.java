package com.yeyamo_mobile.api.media_service.application;

import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
import com.yeyamo_mobile.api.media_service.domain.model.MediaUsageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AUDIO, DOCUMENT and CERTIFICATE validation added in the
 * culture/artisan extension.  Mirrors the style of MediaContentPolicyTests.
 */
class MediaContentPolicyAudioDocumentTests {

    private final MediaContentPolicy policy = new MediaContentPolicy(
            10_000_000L, 100_000_000L,  // image, video
            50_000_000L, 50_000_000L, 10_000_000L, 3600L);  // audio, doc, cert, maxDuration

    // ---- MP3 (ID3 header) --------------------------------------------------

    @Test
    void mp3WithId3HeaderIsAccepted() {
        byte[] mp3 = mp3Id3();
        assertEquals(MediaType.AUDIO, policy.validate("audio/mpeg", mp3));
    }

    @Test
    void mp3WithSyncWordIsAccepted() {
        byte[] mp3 = new byte[16];
        mp3[0] = (byte) 0xff;
        mp3[1] = (byte) 0xfb;
        assertEquals(MediaType.AUDIO, policy.validate("audio/mpeg", mp3));
    }

    // ---- Fake / spoofed audio ----------------------------------------------

    @Test
    void fakeMp3ClaimedAsAudioIsRejected() {
        byte[] fake = "not an mp3 file at all".getBytes();
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validate("audio/mpeg", fake));
        assertEquals("INVALID_MEDIA_CONTENT", ex.getCode());
    }

    @Test
    void imageClaimedAsAudioIsRejected() {
        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validate("audio/mpeg", jpeg));
        assertEquals("INVALID_MEDIA_CONTENT", ex.getCode());
    }

    // ---- OGG ---------------------------------------------------------------

    @Test
    void oggIsAccepted() {
        byte[] ogg = new byte[16];
        ogg[0] = 'O'; ogg[1] = 'g'; ogg[2] = 'g'; ogg[3] = 'S';
        assertEquals(MediaType.AUDIO, policy.validate("audio/ogg", ogg));
    }

    // ---- FLAC --------------------------------------------------------------

    @Test
    void flacIsAccepted() {
        byte[] flac = new byte[16];
        flac[0] = 'f'; flac[1] = 'L'; flac[2] = 'a'; flac[3] = 'C';
        assertEquals(MediaType.AUDIO, policy.validate("audio/flac", flac));
    }

    // ---- WAV ---------------------------------------------------------------

    @Test
    void wavIsAccepted() {
        byte[] wav = new byte[16];
        wav[0]='R'; wav[1]='I'; wav[2]='F'; wav[3]='F';
        wav[8]='W'; wav[9]='A'; wav[10]='V'; wav[11]='E';
        assertEquals(MediaType.AUDIO, policy.validate("audio/wav", wav));
    }

    // ---- PDF (DOCUMENT) ----------------------------------------------------

    @Test
    void pdfIsAcceptedAsDocument() {
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
        assertEquals(MediaType.DOCUMENT, policy.validate("application/pdf", pdf));
    }

    @Test
    void pdfResolvedToCertificateViaUsageType() {
        // PDF resolves to DOCUMENT from MIME; usageType coerces to CERTIFICATE
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
        MediaType raw = policy.validate("application/pdf", pdf);
        assertEquals(MediaType.DOCUMENT, raw);
        // With CERTIFICATE_DOCUMENT usageType → effective type = CERTIFICATE
        MediaType effective = policy.resolveEffectiveType(raw, MediaUsageType.CERTIFICATE_DOCUMENT);
        assertEquals(MediaType.CERTIFICATE, effective);
    }

    // ---- Dangerous file disguised as PDF -----------------------------------

    @Test
    void executableClaimedAsPdfIsRejected() {
        // Windows PE header MZ
        byte[] exe = {0x4d, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validate("application/pdf", exe));
        assertEquals("INVALID_MEDIA_CONTENT", ex.getCode());
    }

    @Test
    void zipClaimedAsPdfIsRejected() {
        // PK header (ZIP / JAR / DOCX)
        byte[] zip = {0x50, 0x4b, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00};
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validate("application/pdf", zip));
        assertEquals("INVALID_MEDIA_CONTENT", ex.getCode());
    }

    // ---- Document MIME whitelist -------------------------------------------

    @Test
    void onlyPdfIsAllowedForDocuments() {
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validateDocumentMime("application/msword"));
        assertEquals("UNSUPPORTED_DOCUMENT_TYPE", ex.getCode());
    }

    @Test
    void pdfMimePassesDocumentWhitelist() {
        assertDoesNotThrow(() -> policy.validateDocumentMime("application/pdf"));
    }

    // ---- Unsupported type --------------------------------------------------

    @Test
    void unknownMimeTypeIsRejected() {
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validate("application/x-custom", new byte[10]));
        assertEquals("UNSUPPORTED_MEDIA_TYPE", ex.getCode());
    }

    // ---- UsageType compatibility -------------------------------------------

    @Test
    void usageTypeMismatchRaisesException() {
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validateUsageType(MediaType.IMAGE, MediaUsageType.ARTISAN_STORY_AUDIO));
        assertEquals("USAGE_TYPE_MISMATCH", ex.getCode());
    }

    @Test
    void compatibleUsageTypePassesValidation() {
        assertDoesNotThrow(() -> policy.validateUsageType(
                MediaType.AUDIO, MediaUsageType.LANGUAGE_PRONUNCIATION));
    }

    @Test
    void nullUsageTypeAlwaysPasses() {
        assertDoesNotThrow(() -> policy.validateUsageType(MediaType.IMAGE, null));
    }

    // ---- Size limits -------------------------------------------------------

    @Test
    void audioSizeTooLargeIsRejected() {
        // maxAudio = 50MB; try to declare 60MB
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validateDeclaredSize("audio/mpeg", 60_000_000L));
        assertEquals("INVALID_MEDIA_SIZE", ex.getCode());
    }

    @Test
    void documentSizeTooLargeIsRejected() {
        // maxDocument = 50MB; try to declare 55MB
        MediaException ex = assertThrows(MediaException.class,
                () -> policy.validateDeclaredSize("application/pdf", 55_000_000L));
        assertEquals("INVALID_MEDIA_SIZE", ex.getCode());
    }

    @Test
    void documentSizeAtLimitPasses() {
        // 50MB exactly at the limit — should pass
        assertDoesNotThrow(() -> policy.validateDeclaredSize("application/pdf", 50_000_000L));
    }

    @Test
    void imageSizeAtLimitPasses() {
        assertDoesNotThrow(() -> policy.validateDeclaredSize("image/jpeg", 10_000_000L));
    }

    // ---- Helpers -----------------------------------------------------------

    /** Minimal valid ID3 header (bytes 0-2 = "ID3"). */
    private byte[] mp3Id3() {
        byte[] b = new byte[16];
        b[0] = 'I'; b[1] = 'D'; b[2] = '3';
        b[3] = 3; b[4] = 0;  // version 2.3.0
        return b;
    }
}
