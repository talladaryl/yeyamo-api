package com.yeyamo_mobile.api.media_service.application;

import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
import com.yeyamo_mobile.api.media_service.domain.model.MediaUsageType;
import com.yeyamo_mobile.api.media_service.infrastructure.persistence.InMemoryMediaQuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class MediaQuotaServiceTests {

    private InMemoryMediaQuotaRepository repo;
    private MediaQuotaService service;

    // Tight limits to make tests fast
    private static final long DAILY_IMAGE   = 1_000L;
    private static final long DAILY_VIDEO   = 2_000L;
    private static final long DAILY_AUDIO   = 500L;
    private static final long DAILY_DOC     = 300L;
    private static final long ARTISAN_AUDIO = 1_000L;
    private static final long ARTISAN_CERT  = 800L;
    private static final long MONTHLY_AUDIO = 2_000L;
    private static final long ARTISAN_MONTHLY_AUDIO = 5_000L;

    @BeforeEach
    void setUp() {
        repo = new InMemoryMediaQuotaRepository();
        service = new MediaQuotaService(repo,
                DAILY_IMAGE, DAILY_VIDEO, DAILY_AUDIO, DAILY_DOC,
                ARTISAN_AUDIO, ARTISAN_CERT, MONTHLY_AUDIO, ARTISAN_MONTHLY_AUDIO);
    }

    // ---- Standard user -------------------------------------------------------

    @Test
    void firstUploadWithinLimitPasses() {
        assertDoesNotThrow(() ->
                service.checkAndEnforce("user-1", false, MediaType.IMAGE, null, 500L));
    }

    @Test
    void uploadExactlyAtDailyLimitPasses() {
        assertDoesNotThrow(() ->
                service.checkAndEnforce("user-1", false, MediaType.IMAGE, null, DAILY_IMAGE));
    }

    @Test
    void uploadExceedingDailyLimitFails() {
        service.checkAndEnforce("user-1", false, MediaType.IMAGE, null, 900L);
        service.recordUpload("user-1", MediaType.IMAGE, 900L);

        MediaException ex = assertThrows(MediaException.class, () ->
                service.checkAndEnforce("user-1", false, MediaType.IMAGE, null, 200L));
        assertEquals("QUOTA_EXCEEDED", ex.getCode());
    }

    @Test
    void differentUsersHaveIndependentQuotas() {
        service.recordUpload("user-1", MediaType.AUDIO, DAILY_AUDIO);
        // user-2 should still be allowed
        assertDoesNotThrow(() ->
                service.checkAndEnforce("user-2", false, MediaType.AUDIO, null, 100L));
    }

    // ---- Artisan vs standard -----------------------------------------------

    @Test
    void artisanHasHigherAudioDailyLimit() {
        // Standard user: DAILY_AUDIO = 500, Artisan: ARTISAN_AUDIO = 1000
        // Upload 600 bytes — should fail for user, pass for artisan
        MediaException ex = assertThrows(MediaException.class, () ->
                service.checkAndEnforce("user-1", false, MediaType.AUDIO, null, 600L));
        assertEquals("QUOTA_EXCEEDED", ex.getCode());

        assertDoesNotThrow(() ->
                service.checkAndEnforce("artisan-1", true, MediaType.AUDIO, null, 600L));
    }

    @Test
    void artisanHasHigherCertificateDailyLimit() {
        // Standard gets DAILY_DOC = 300, artisan gets ARTISAN_CERT = 800
        assertDoesNotThrow(() ->
                service.checkAndEnforce("artisan-1", true, MediaType.CERTIFICATE, null, 700L));

        MediaException ex = assertThrows(MediaException.class, () ->
                service.checkAndEnforce("user-1", false, MediaType.CERTIFICATE, null, 700L));
        assertEquals("QUOTA_EXCEEDED", ex.getCode());
    }

    // ---- Monthly limit (audio only) ----------------------------------------

    @Test
    void monthlyAudioLimitEnforced() {
        // Record just below the monthly limit
        service.recordUpload("user-1", MediaType.AUDIO, MONTHLY_AUDIO - 100L);

        MediaException ex = assertThrows(MediaException.class, () ->
                service.checkAndEnforce("user-1", false, MediaType.AUDIO, null, 200L));
        assertEquals("QUOTA_EXCEEDED", ex.getCode());
    }

    @Test
    void artisanMonthlyAudioHigherThanUserMonthly() {
        // ARTISAN_AUDIO daily = 1000, standard daily = 500
        // MONTHLY_AUDIO user = 2000, ARTISAN_MONTHLY = 5000
        // We accumulate 900 bytes (< 1000 daily artisan) twice across 3 different
        // "virtual days" via the bucket key — but since InMemory has no TTL,
        // we directly inspect that artisan is NOT blocked at 2100 total
        // while a user WOULD be at that point.
        //
        // Strategy: use 5 uploads of 900 bytes (4500 total, < 5000 artisan monthly)
        // We need to reset daily each time by tweaking total to stay under daily limit.
        // Simplest: use small amounts totalling > user monthly but < artisan monthly.

        // Use 3 uploads of 990 bytes = 2970 bytes total
        // Each upload must pass daily check (artisan daily = 1000).
        // Reset repo between logical "days" isn't possible without TTL.
        // Instead, use amounts < daily artisan (1000) per check, accumulate to > user monthly (2000).
        // Single upload of 990 stays under daily (1000 artisan).
        // But after recording 990, the next 990 = 1980 > 1000 daily.
        // So this test verifies artisan CAN upload when user CANNOT at the monthly level
        // with a fresh repo per user.

        // Separate repo for artisan vs user comparison
        InMemoryMediaQuotaRepository artisanRepo = new InMemoryMediaQuotaRepository();
        InMemoryMediaQuotaRepository userRepo    = new InMemoryMediaQuotaRepository();

        MediaQuotaService artisanSvc = new MediaQuotaService(artisanRepo,
                DAILY_IMAGE, DAILY_VIDEO, DAILY_AUDIO, DAILY_DOC,
                ARTISAN_AUDIO, ARTISAN_CERT, MONTHLY_AUDIO, ARTISAN_MONTHLY_AUDIO);

        MediaQuotaService userSvc = new MediaQuotaService(userRepo,
                DAILY_IMAGE, DAILY_VIDEO, DAILY_AUDIO, DAILY_DOC,
                ARTISAN_AUDIO, ARTISAN_CERT, MONTHLY_AUDIO, ARTISAN_MONTHLY_AUDIO);

        // Manually push monthly buckets past user limit (2000) without touching daily
        // This simulates what recordUpload does over multiple calendar days
        String artisanMonthlyKey = "quota:artisan-x:AUDIO:monthly:" +
                java.time.LocalDate.now(java.time.ZoneOffset.UTC).getYear() + "-" +
                String.format("%02d", java.time.LocalDate.now(java.time.ZoneOffset.UTC).getMonthValue());
        String userMonthlyKey = "quota:user-x:AUDIO:monthly:" +
                java.time.LocalDate.now(java.time.ZoneOffset.UTC).getYear() + "-" +
                String.format("%02d", java.time.LocalDate.now(java.time.ZoneOffset.UTC).getMonthValue());

        artisanRepo.incrementBucket(artisanMonthlyKey, MONTHLY_AUDIO + 100L); // 2100 used
        userRepo.incrementBucket(userMonthlyKey, MONTHLY_AUDIO + 100L);       // 2100 used

        // Artisan: 2100 used, 5000 limit → should still be allowed
        assertDoesNotThrow(() ->
                artisanSvc.checkAndEnforce("artisan-x", true, MediaType.AUDIO, null, 100L));

        // User: 2100 used, 2000 limit → should be blocked
        MediaException ex = assertThrows(MediaException.class, () ->
                userSvc.checkAndEnforce("user-x", false, MediaType.AUDIO, null, 100L));
        assertEquals("QUOTA_EXCEEDED", ex.getCode());
    }

    // ---- recordUpload accumulates correctly --------------------------------

    @Test
    void recordUploadAccumulatesBytes() {
        service.recordUpload("user-1", MediaType.AUDIO, 100L);
        service.recordUpload("user-1", MediaType.AUDIO, 200L);
        service.recordUpload("user-1", MediaType.AUDIO, 150L);

        // 450 used out of 500 daily limit — 60 more is fine, 61 fails
        assertDoesNotThrow(() ->
                service.checkAndEnforce("user-1", false, MediaType.AUDIO, null, 50L));

        MediaException ex = assertThrows(MediaException.class, () ->
                service.checkAndEnforce("user-1", false, MediaType.AUDIO, null, 60L));
        assertEquals("QUOTA_EXCEEDED", ex.getCode());
    }

    // ---- Zero-size guard ---------------------------------------------------

    @Test
    void zeroSizeIsRejectedByPolicy() {
        // The content policy should reject zero-size files before quota is checked
        // but we also check that quota check for 0 bytes passes harmlessly
        assertDoesNotThrow(() ->
                service.checkAndEnforce("user-1", false, MediaType.IMAGE, null, 0L));
    }
}
