package com.yeyamo_mobile.api.media_service.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SignedUrlServiceTests {

    private SignedUrlService service;
    private static final String SECRET = "testSecretKey123456789012345678901234567890";

    @BeforeEach
    void setUp() {
        service = new SignedUrlService(SECRET, 60L);
    }

    // ---- URL generation ----------------------------------------------------

    @Test
    void generateReturnsUrlWithExpiresAndSig() {
        UUID id = UUID.randomUUID();
        String url = service.generate(id, "/api/v1/media", null);

        assertTrue(url.contains("/api/v1/media/" + id + "/content"));
        assertTrue(url.contains("expires="));
        assertTrue(url.contains("sig="));
    }

    @Test
    void customTtlIsReflectedInUrl() {
        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis() / 1000;

        String url = service.generate(id, "/api/v1/media", 120L); // 2 hours

        // Extract expires value
        String expiresStr = url.substring(url.indexOf("expires=") + 8,
                url.indexOf("&sig="));
        long expires = Long.parseLong(expiresStr);

        // Should be roughly now + 120 minutes
        assertTrue(expires > now + 7000); // at least 7000s in the future
        assertTrue(expires < now + 8000); // at most 8000s
    }

    // ---- Validation --------------------------------------------------------

    @Test
    void validUrlPassesValidation() {
        UUID id = UUID.randomUUID();
        String url = service.generate(id, "/api/v1/media", null);

        long expires = Long.parseLong(extractParam(url, "expires"));
        String sig   = extractParam(url, "sig");

        assertDoesNotThrow(() -> service.validate(id, expires, sig));
    }

    @Test
    void tamperedSignatureIsRejected() {
        UUID id = UUID.randomUUID();
        String url = service.generate(id, "/api/v1/media", null);

        long expires = Long.parseLong(extractParam(url, "expires"));
        String badSig = "0".repeat(64); // wrong sig

        MediaException ex = assertThrows(MediaException.class,
                () -> service.validate(id, expires, badSig));
        assertEquals("SIGNED_URL_INVALID", ex.getCode());
    }

    @Test
    void expiredUrlIsRejected() {
        UUID id = UUID.randomUUID();
        long pastEpoch = System.currentTimeMillis() / 1000 - 3600; // 1 hour ago

        // Build a "valid" sig for the expired payload
        SignedUrlService svc2 = new SignedUrlService(SECRET, 60L);
        String url = svc2.generate(id, "/api/v1/media", null);
        String sig  = extractParam(url, "sig");

        MediaException ex = assertThrows(MediaException.class,
                () -> service.validate(id, pastEpoch, sig));
        assertEquals("SIGNED_URL_EXPIRED", ex.getCode());
    }

    @Test
    void signatureForDifferentMediaIdIsRejected() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        String url = service.generate(id1, "/api/v1/media", null);
        long expires = Long.parseLong(extractParam(url, "expires"));
        String sig   = extractParam(url, "sig");

        // Using id1's sig to validate id2
        MediaException ex = assertThrows(MediaException.class,
                () -> service.validate(id2, expires, sig));
        assertEquals("SIGNED_URL_INVALID", ex.getCode());
    }

    @Test
    void nullSignatureIsRejected() {
        UUID id = UUID.randomUUID();
        long futureEpoch = System.currentTimeMillis() / 1000 + 3600;

        MediaException ex = assertThrows(MediaException.class,
                () -> service.validate(id, futureEpoch, null));
        assertEquals("SIGNED_URL_INVALID", ex.getCode());
    }

    // ---- Helpers -----------------------------------------------------------

    private String extractParam(String url, String param) {
        int start = url.indexOf(param + "=") + param.length() + 1;
        int end   = url.indexOf("&", start);
        return end == -1 ? url.substring(start) : url.substring(start, end);
    }
}
