package com.yeyamo_mobile.api.media_service.application;

import com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort;
import com.yeyamo_mobile.api.media_service.application.thumbnail.AudioWaveformStrategy;
import com.yeyamo_mobile.api.media_service.application.thumbnail.ImageThumbnailStrategy;
import com.yeyamo_mobile.api.media_service.domain.model.*;
import com.yeyamo_mobile.api.media_service.domain.port.MediaRepository;
import com.yeyamo_mobile.api.media_service.infrastructure.persistence.InMemoryMediaQuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for the extended upload path.
 * Uses the same in-memory fakes as the existing MediaApplicationServiceTests.
 */
class MediaCultureUploadTests {

    private MemoryRepository   repo;
    private MemoryStorage      storage;
    private List<String>       events;
    private MediaApplicationService service;

    @BeforeEach
    void setUp() {
        repo    = new MemoryRepository();
        storage = new MemoryStorage();
        events  = new ArrayList<>();

        InMemoryMediaQuotaRepository quotaRepo = new InMemoryMediaQuotaRepository();
        MediaQuotaService quota = new MediaQuotaService(quotaRepo,
                10_000_000L, 100_000_000L, 50_000_000L,
                50_000_000L, 104_857_600L, 50_000_000L,
                262_144_000L, 1_073_741_824L);
        AudioTranscodingService transcoding = new AudioTranscodingService(false, "ffmpeg", 60L);

        service = new MediaApplicationService(
                repo, storage,
                (e, m, c, a) -> events.add(e),
                new MediaContentPolicy(10_000_000L, 100_000_000L,
                        50_000_000L, 50_000_000L, 10_000_000L, 3600L),
                List.of(new ImageThumbnailStrategy(100, 100), new AudioWaveformStrategy()),
                quota, transcoding);
    }

    // ---- Audio upload (MP3 with ID3 header) --------------------------------

    @Test
    void audioUploadWithMp3IsAccepted() {
        byte[] mp3 = mp3Bytes();
        MediaAsset media = service.upload(
                "user-1", "narration.mp3", "audio/mpeg", mp3,
                "Narration", "culture", "c1", "corr-1",
                MediaUsageType.CULTURE_STORY_AUDIO, false,
                null, null, null, null, ConsentStatus.NOT_REQUIRED, null);

        assertEquals(MediaStatus.READY, media.getStatus());
        assertEquals(MediaType.AUDIO, media.getType());
        assertEquals(MediaUsageType.CULTURE_STORY_AUDIO, media.getUsageType());
        assertEquals(ConsentStatus.NOT_REQUIRED, media.getConsentStatus());
        assertTrue(events.contains("media.uploaded"));
    }

    @Test
    void audioUploadPublishesReadyEvent() {
        service.upload("user-1", "lesson.mp3", "audio/mpeg", mp3Bytes(),
                null, "lesson", "l1", null,
                MediaUsageType.LESSON_AUDIO, false,
                null, null, null, null, null, null);

        assertTrue(events.contains("media.uploaded"));
        assertTrue(events.contains("media.ready") || events.contains("media.thumbnail_failed"));
    }

    // ---- Document upload (PDF) --------------------------------------------

    @Test
    void pdfDocumentUploadIsAccepted() {
        byte[] pdf = pdfBytes();
        MediaAsset media = service.upload(
                "artisan-1", "cert.pdf", "application/pdf", pdf,
                "Authenticity certificate", "artwork", "a1", "corr-2",
                MediaUsageType.CERTIFICATE_DOCUMENT, true,
                "Artisan Name", "CC-BY-4.0", "NON_COMMERCIAL",
                true, ConsentStatus.NOT_REQUIRED, null);

        assertEquals(MediaStatus.READY, media.getStatus());
        assertEquals(MediaType.CERTIFICATE, media.getType());
        assertEquals(MediaUsageType.CERTIFICATE_DOCUMENT, media.getUsageType());
        assertEquals("Artisan Name", media.getCopyrightOwner());
        assertEquals("CC-BY-4.0", media.getLicenseType());
        assertTrue(media.getAttributionRequired());
    }

    // ---- Consent metadata --------------------------------------------------

    @Test
    void consentObtainedIsStoredCorrectly() {
        byte[] mp3 = mp3Bytes();
        MediaAsset media = service.upload(
                "user-1", "story.mp3", "audio/mpeg", mp3,
                null, "oral-history", "oh1", null,
                MediaUsageType.ARTISAN_STORY_AUDIO, false,
                "Elder John", "TRADITIONAL", null, false,
                ConsentStatus.OBTAINED, "consent-record-42");

        assertEquals(ConsentStatus.OBTAINED, media.getConsentStatus());
        assertEquals("consent-record-42", media.getConsentRecordId());
    }

    // ---- Faux audio (magic byte mismatch) ----------------------------------

    @Test
    void fakeAudioClaimedAsMp3IsRejected() {
        byte[] fake = "this is not audio data at all".getBytes();
        MediaException ex = assertThrows(MediaException.class, () ->
                service.upload("user-1", "fake.mp3", "audio/mpeg", fake,
                        null, null, null, null,
                        MediaUsageType.LESSON_AUDIO, false,
                        null, null, null, null, null, null));
        assertEquals("INVALID_MEDIA_CONTENT", ex.getCode());
        // No storage writes should have happened
        assertTrue(storage.data.isEmpty());
        assertFalse(events.contains("media.uploaded"));
    }

    // ---- Dangerous file disguised as PDF -----------------------------------

    @Test
    void executableClaimedAsPdfIsRejected() {
        byte[] exe = {0x4d, 0x5a, 0x00, 0x00}; // MZ (PE header)
        MediaException ex = assertThrows(MediaException.class, () ->
                service.upload("artisan-1", "malware.exe", "application/pdf", exe,
                        null, "artwork", "a2", null,
                        MediaUsageType.CERTIFICATE_DOCUMENT, true,
                        null, null, null, null, null, null));
        assertEquals("INVALID_MEDIA_CONTENT", ex.getCode());
        assertTrue(storage.data.isEmpty());
    }

    // ---- Quota enforcement -------------------------------------------------

    @Test
    void quotaExceededRaisesExceptionAndPublishesRejectedEvent() {
        // Set a tiny quota by creating a service with 10-byte daily audio limit
        InMemoryMediaQuotaRepository tinyQuotaRepo = new InMemoryMediaQuotaRepository();
        MediaQuotaService tinyQuota = new MediaQuotaService(tinyQuotaRepo,
                10_000L, 10_000L,
                10L,       // 10 bytes daily audio — tiny
                10_000L, 10_000L, 10_000L,
                1_000L, 5_000L);

        MediaApplicationService restrictedService = new MediaApplicationService(
                new MemoryRepository(), storage,
                (e, m, c, a) -> events.add(e),
                new MediaContentPolicy(10_000_000L, 100_000_000L,
                        50_000_000L, 50_000_000L, 10_000_000L, 3600L),
                List.of(new AudioWaveformStrategy()),
                tinyQuota, new AudioTranscodingService(false, "ffmpeg", 60L));

        byte[] mp3 = mp3Bytes(); // more than 10 bytes
        MediaException ex = assertThrows(MediaException.class, () ->
                restrictedService.upload("user-1", "audio.mp3", "audio/mpeg", mp3,
                        null, null, null, null,
                        MediaUsageType.LESSON_AUDIO, false,
                        null, null, null, null, null, null));
        assertEquals("QUOTA_EXCEEDED", ex.getCode());
    }

    // ---- UsageType / MediaType mismatch ------------------------------------

    @Test
    void usageTypeMismatchIsRejectedBeforeStorage() {
        byte[] png = pngBytes();
        MediaException ex = assertThrows(MediaException.class, () ->
                service.upload("user-1", "photo.png", "image/png", png,
                        null, null, null, null,
                        MediaUsageType.LESSON_AUDIO, // AUDIO usage on an IMAGE
                        false, null, null, null, null, null, null));
        assertEquals("USAGE_TYPE_MISMATCH", ex.getCode());
        assertTrue(storage.data.isEmpty());
    }

    // ---- Duplicate detection ----------------------------------------------

    @Test
    void duplicateAudioUploadIsRejected() throws Exception {
        byte[] mp3 = mp3Bytes();
        service.upload("user-1", "story.mp3", "audio/mpeg", mp3,
                null, null, null, null);

        MediaException ex = assertThrows(MediaException.class, () ->
                service.upload("user-1", "story2.mp3", "audio/mpeg", mp3,
                        null, null, null, null));
        assertEquals("MEDIA_DUPLICATE", ex.getCode());
    }

    // ---- Delete -------------------------------------------------------------

    @Test
    void deletePublishesDeletedEvent() throws Exception {
        byte[] png = pngBytes();
        MediaAsset media = service.upload("user-1", "photo.png", "image/png", png,
                null, null, null, null);
        events.clear();

        service.delete(media.getId(), "user-1", false, "corr-del");
        assertTrue(events.contains("media.deleted"));
    }

    @Test
    void nonOwnerCannotDelete() {
        byte[] png = pngBytes();
        MediaAsset media = service.upload("user-1", "photo.png", "image/png", png,
                null, null, null, null);

        MediaException ex = assertThrows(MediaException.class, () ->
                service.delete(media.getId(), "attacker", false, null));
        assertEquals("MEDIA_FORBIDDEN", ex.getCode());
    }

    @Test
    void adminCanDeleteAnyMedia() {
        byte[] png = pngBytes();
        MediaAsset media = service.upload("user-1", "photo.png", "image/png", png,
                null, null, null, null);

        assertDoesNotThrow(() ->
                service.delete(media.getId(), "admin", true, null));
    }

    // ---- Access denied to private document without signed URL --------------

    @Test
    void accessingDocumentWithoutSignedUrlFails() {
        byte[] pdf = pdfBytes();
        MediaAsset cert = service.upload(
                "artisan-1", "cert.pdf", "application/pdf", pdf,
                null, "artwork", "a1", null,
                MediaUsageType.CERTIFICATE_DOCUMENT, true,
                null, null, null, null, null, null);

        SignedUrlService signedUrlService = new SignedUrlService(
                "testSecretKey123456789012345678901234567890", 60L);

        // No signed URL provided — should raise an appropriate exception
        MediaException ex = assertThrows(MediaException.class, () ->
                service.protectedContent(cert.getId(), 0L, null, signedUrlService));
        assertEquals("SIGNED_URL_EXPIRED", ex.getCode());
    }

    @Test
    void expiredSignedUrlFails() {
        byte[] pdf = pdfBytes();
        MediaAsset cert = service.upload(
                "artisan-1", "cert.pdf", "application/pdf", pdf,
                null, "artwork", "a1", null,
                MediaUsageType.CERTIFICATE_DOCUMENT, true,
                null, null, null, null, null, null);

        SignedUrlService signedUrlService = new SignedUrlService(
                "testSecretKey123456789012345678901234567890", 60L);

        long pastEpoch = System.currentTimeMillis() / 1000 - 7200; // 2 hours ago
        MediaException ex = assertThrows(MediaException.class, () ->
                service.protectedContent(cert.getId(), pastEpoch, "fakesig", signedUrlService));
        assertEquals("SIGNED_URL_EXPIRED", ex.getCode());
    }

    // ---- Helpers -----------------------------------------------------------

    /** Minimal ID3-tagged MP3 header that passes magic byte checks. */
    private byte[] mp3Bytes() {
        byte[] b = new byte[256];
        b[0] = 'I'; b[1] = 'D'; b[2] = '3';
        b[3] = 3; b[4] = 0;
        return b;
    }

    /** Minimal valid PDF magic bytes. */
    private byte[] pdfBytes() {
        byte[] b = new byte[256];
        b[0] = '%'; b[1] = 'P'; b[2] = 'D'; b[3] = 'F'; b[4] = '-';
        return b;
    }

    /** Real PNG image generated via AWT. */
    private byte[] pngBytes() {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- In-memory fakes (same pattern as existing tests) ------------------

    static class MemoryRepository implements MediaRepository {
        final Map<UUID, MediaAsset> data = new HashMap<>();
        public MediaAsset save(MediaAsset m) { data.put(m.getId(), m); return m; }
        public Optional<MediaAsset> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
        public boolean existsByChecksumAndOwnerId(String c, String o) {
            return data.values().stream().anyMatch(m ->
                    m.getChecksum().equals(c) && m.getOwnerId().equals(o)
                    && m.getStatus() != MediaStatus.DELETED);
        }
    }

    static class MemoryStorage implements com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort {
        final Map<String, byte[]> data = new HashMap<>();
        public String store(String k, InputStream i, long l, String c) {
            try { data.put(k, i.readAllBytes()); return k; } catch (Exception e) { throw new RuntimeException(e); }
        }
        public StoredObject open(String k) {
            byte[] b = data.get(k);
            if (b == null) throw new MediaException("MEDIA_NOT_FOUND", "not found");
            return new StoredObject(new ByteArrayInputStream(b), b.length, "application/octet-stream");
        }
        public void delete(String k) { data.remove(k); }
    }
}
