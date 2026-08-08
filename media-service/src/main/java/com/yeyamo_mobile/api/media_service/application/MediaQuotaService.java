package com.yeyamo_mobile.api.media_service.application;

import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
import com.yeyamo_mobile.api.media_service.domain.model.MediaUsageType;
import com.yeyamo_mobile.api.media_service.domain.port.MediaQuotaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Enforces upload quotas per owner, media type and time period.
 *
 * <p>Quotas:
 * <ul>
 *   <li>Standard user: configurable daily / monthly byte limits per type</li>
 *   <li>Artisan (AUDIO, CERTIFICATE): higher limits, configurable</li>
 *   <li>Quotas are checked <em>before</em> the upload is processed so storage
 *       is never consumed for a rejected request.</li>
 * </ul>
 * </p>
 *
 * <p>The implementation is intentionally simple: it delegates counting to the
 * repository and raises {@link MediaException} with code {@code QUOTA_EXCEEDED}
 * when a limit would be breached.</p>
 */
@Service
public class MediaQuotaService {

    // ---- Default limits (bytes) per day ------------------------------------
    private final long userDailyImageBytes;
    private final long userDailyVideoBytes;
    private final long userDailyAudioBytes;
    private final long userDailyDocumentBytes;
    private final long artisanDailyAudioBytes;
    private final long artisanDailyCertificateBytes;
    // Monthly byte limits
    private final long userMonthlyAudioBytes;
    private final long artisanMonthlyAudioBytes;

    private final MediaQuotaRepository quotaRepo;

    public MediaQuotaService(
            MediaQuotaRepository quotaRepo,
            @Value("${media.quota.user.daily.image-bytes:52428800}")      long userDailyImageBytes,
            @Value("${media.quota.user.daily.video-bytes:524288000}")     long userDailyVideoBytes,
            @Value("${media.quota.user.daily.audio-bytes:26214400}")      long userDailyAudioBytes,
            @Value("${media.quota.user.daily.document-bytes:26214400}")   long userDailyDocumentBytes,
            @Value("${media.quota.artisan.daily.audio-bytes:104857600}")  long artisanDailyAudioBytes,
            @Value("${media.quota.artisan.daily.certificate-bytes:52428800}") long artisanDailyCertBytes,
            @Value("${media.quota.user.monthly.audio-bytes:262144000}")   long userMonthlyAudioBytes,
            @Value("${media.quota.artisan.monthly.audio-bytes:1073741824}") long artisanMonthlyAudioBytes) {
        this.quotaRepo = quotaRepo;
        this.userDailyImageBytes          = userDailyImageBytes;
        this.userDailyVideoBytes          = userDailyVideoBytes;
        this.userDailyAudioBytes          = userDailyAudioBytes;
        this.userDailyDocumentBytes       = userDailyDocumentBytes;
        this.artisanDailyAudioBytes       = artisanDailyAudioBytes;
        this.artisanDailyCertificateBytes = artisanDailyCertBytes;
        this.userMonthlyAudioBytes        = userMonthlyAudioBytes;
        this.artisanMonthlyAudioBytes     = artisanMonthlyAudioBytes;
    }

    /**
     * Check and enforce quota. Raises {@link MediaException} if exceeded.
     *
     * @param ownerId   user or artisan identifier
     * @param isArtisan true if the owner has the artisan role
     * @param type      resolved media type
     * @param usageType optional usage type for finer-grained limits
     * @param sizeBytes size of the file being uploaded
     */
    public void checkAndEnforce(
            String ownerId, boolean isArtisan, MediaType type,
            MediaUsageType usageType, long sizeBytes) {

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String bucketKey = bucketKey(ownerId, type, today);

        // Daily quota
        long dailyUsed = quotaRepo.sumBytesForBucket(bucketKey);
        long dailyLimit = dailyLimit(isArtisan, type);
        if (dailyUsed + sizeBytes > dailyLimit) {
            throw new MediaException("QUOTA_EXCEEDED",
                "Daily upload quota exceeded for type " + type +
                " (used " + dailyUsed + ", limit " + dailyLimit + ")");
        }

        // Monthly quota for AUDIO (most sensitive)
        if (type == MediaType.AUDIO) {
            String monthlyBucket = monthlyBucketKey(ownerId, type, today);
            long monthlyUsed = quotaRepo.sumBytesForBucket(monthlyBucket);
            long monthlyLimit = isArtisan ? artisanMonthlyAudioBytes : userMonthlyAudioBytes;
            if (monthlyUsed + sizeBytes > monthlyLimit) {
                throw new MediaException("QUOTA_EXCEEDED",
                    "Monthly audio upload quota exceeded");
            }
        }
    }

    /**
     * Records a successful upload against the quota buckets.
     * Called after the upload is committed.
     */
    public void recordUpload(String ownerId, MediaType type, long sizeBytes) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        quotaRepo.incrementBucket(bucketKey(ownerId, type, today), sizeBytes);
        if (type == MediaType.AUDIO) {
            quotaRepo.incrementBucket(monthlyBucketKey(ownerId, type, today), sizeBytes);
        }
    }

    // -------------------------------------------------------------------------

    private long dailyLimit(boolean artisan, MediaType type) {
        return switch (type) {
            case IMAGE       -> userDailyImageBytes;
            case VIDEO       -> userDailyVideoBytes;
            case AUDIO       -> artisan ? artisanDailyAudioBytes : userDailyAudioBytes;
            case DOCUMENT    -> userDailyDocumentBytes;
            case CERTIFICATE -> artisan ? artisanDailyCertificateBytes : userDailyDocumentBytes;
        };
    }

    private String bucketKey(String owner, MediaType type, LocalDate date) {
        return String.format(Locale.ROOT, "quota:%s:%s:%s", owner, type.name(), date);
    }

    private String monthlyBucketKey(String owner, MediaType type, LocalDate date) {
        return String.format(Locale.ROOT, "quota:%s:%s:monthly:%d-%02d",
                owner, type.name(), date.getYear(), date.getMonthValue());
    }
}
