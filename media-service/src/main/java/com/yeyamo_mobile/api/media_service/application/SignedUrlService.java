package com.yeyamo_mobile.api.media_service.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Generates and validates time-limited signed URLs for sensitive media
 * (DOCUMENT, CERTIFICATE types).
 *
 * <p>A signed URL encodes the media ID and expiry and is protected with an
 * HMAC-SHA256 signature so it cannot be forged or extended.  The signature
 * uses the same shared JWT secret, keeping the key surface minimal.</p>
 *
 * <p>Format: {@code /api/v1/media/{id}/content?expires={epoch}&sig={hex}}</p>
 */
@Service
public class SignedUrlService {

    private final String secret;
    private final long defaultTtlMinutes;
    private static final String ALGO = "HmacSHA256";

    public SignedUrlService(
            @Value("${jwt.secret}") String secret,
            @Value("${media.signed-url.ttl-minutes:60}") long defaultTtlMinutes) {
        this.secret = secret;
        this.defaultTtlMinutes = defaultTtlMinutes;
    }

    /**
     * Build a signed URL for a protected media asset.
     *
     * @param mediaId   UUID of the asset
     * @param baseUrl   e.g. {@code /api/v1/media}
     * @param ttlMinutes expiry window; pass {@code null} to use default
     * @return complete URL string including query params
     */
    public String generate(UUID mediaId, String baseUrl, Long ttlMinutes) {
        long ttl = ttlMinutes != null ? ttlMinutes : defaultTtlMinutes;
        long expiresEpoch = Instant.now().plus(ttl, ChronoUnit.MINUTES).getEpochSecond();
        String payload = mediaId + ":" + expiresEpoch;
        String sig = sign(payload);
        return baseUrl + "/" + mediaId + "/content?expires=" + expiresEpoch + "&sig=" + sig;
    }

    /**
     * Validate a signed URL.
     *
     * @param mediaId      UUID extracted from the path
     * @param expiresEpoch value from the {@code expires} query parameter
     * @param sig          value from the {@code sig} query parameter
     * @throws MediaException with code {@code SIGNED_URL_INVALID} or {@code SIGNED_URL_EXPIRED}
     */
    public void validate(UUID mediaId, long expiresEpoch, String sig) {
        if (Instant.now().getEpochSecond() > expiresEpoch) {
            throw new MediaException("SIGNED_URL_EXPIRED", "This media URL has expired");
        }
        String payload = mediaId + ":" + expiresEpoch;
        String expected = sign(payload);
        if (!constantTimeEquals(expected, sig)) {
            throw new MediaException("SIGNED_URL_INVALID", "Media URL signature is invalid");
        }
    }

    // -------------------------------------------------------------------------

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign media URL", e);
        }
    }

    /** Constant-time string comparison to prevent timing attacks. */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) return false;
        int diff = 0;
        for (int i = 0; i < ba.length; i++) diff |= ba[i] ^ bb[i];
        return diff == 0;
    }
}
