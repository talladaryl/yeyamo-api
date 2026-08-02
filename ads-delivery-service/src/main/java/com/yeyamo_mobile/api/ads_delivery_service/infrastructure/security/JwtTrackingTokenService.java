package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.security;

import com.yeyamo_mobile.api.ads_delivery_service.application.dto.TrackingTokenData;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.TrackingTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * JWT-like tracking token service with HMAC signature
 * Format: deliveryId.campaignId.userId.expiresAt.signature
 */
@Service
public class JwtTrackingTokenService implements TrackingTokenService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String IMPRESSION_PREFIX = "IMP";
    private static final String CLICK_PREFIX = "CLK";

    @Value("${yeyamo.ads.tracking-token.secret}")
    private String secret;

    @Override
    public String generateImpressionToken(String deliveryId, String campaignId, String userId, Instant expiresAt) {
        return generateToken(IMPRESSION_PREFIX, deliveryId, campaignId, userId, expiresAt);
    }

    @Override
    public String generateClickToken(String deliveryId, String campaignId, String userId, Instant expiresAt) {
        return generateToken(CLICK_PREFIX, deliveryId, campaignId, userId, expiresAt);
    }

    @Override
    public boolean verifyImpressionToken(String token) {
        return verifyToken(token, IMPRESSION_PREFIX);
    }

    @Override
    public boolean verifyClickToken(String token) {
        return verifyToken(token, CLICK_PREFIX);
    }

    @Override
    public TrackingTokenData decodeImpressionToken(String token) {
        return decodeToken(token);
    }

    @Override
    public TrackingTokenData decodeClickToken(String token) {
        return decodeToken(token);
    }

    private String generateToken(String prefix, String deliveryId, String campaignId, String userId, Instant expiresAt) {
        String userIdPart = userId != null ? userId : "anonymous";
        String payload = String.format("%s.%s.%s.%s.%d",
                prefix, deliveryId, campaignId, userIdPart, expiresAt.getEpochSecond());

        String signature = generateSignature(payload);
        String tokenContent = payload + "." + signature;

        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenContent.getBytes(StandardCharsets.UTF_8));
    }

    private boolean verifyToken(String token, String expectedPrefix) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");

            if (parts.length != 6) {
                return false;
            }

            String prefix = parts[0];
            if (!prefix.equals(expectedPrefix)) {
                return false;
            }

            // Check expiration
            long expiresAt = Long.parseLong(parts[4]);
            if (Instant.now().getEpochSecond() > expiresAt) {
                return false;
            }

            // Verify signature
            String payload = String.join(".", parts[0], parts[1], parts[2], parts[3], parts[4]);
            String expectedSignature = generateSignature(payload);
            String actualSignature = parts[5];

            return expectedSignature.equals(actualSignature);

        } catch (Exception e) {
            return false;
        }
    }

    private TrackingTokenData decodeToken(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\.");

            if (parts.length != 6) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String deliveryId = parts[1];
            String campaignId = parts[2];
            String userId = parts[3].equals("anonymous") ? null : parts[3];
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(parts[4]));

            return new TrackingTokenData(deliveryId, campaignId, userId, expiresAt);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode token", e);
        }
    }

    private String generateSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
}
