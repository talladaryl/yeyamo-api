package com.yeyamo_mobile.api.discovery_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.discovery_service.application.DiscoveryProjectionService;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CultureDiscoveryEventConsumerTest {

    private DiscoveryProjectionService projections;
    private ProcessedEventRepository processed;
    private CultureDiscoveryEventConsumer consumer;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        projections = mock(DiscoveryProjectionService.class);
        processed   = mock(ProcessedEventRepository.class);
        consumer    = new CultureDiscoveryEventConsumer(mapper, projections, processed);
        when(processed.existsById(any())).thenReturn(false);
        when(projections.find(any())).thenReturn(Optional.empty());
    }

    // -------------------------------------------------------------------------
    // CultureContentPublished
    // -------------------------------------------------------------------------

    @Test
    void cultureContentPublished_projects_document() throws Exception {
        String raw = event("CultureContentPublished", Map.of(
                "contentId",    "cnt-1",
                "title",        "Masques Dogon",
                "status",       "PUBLISHED",
                "contentType",  "CULTURE",
                "countryCode",  "ML",
                "languageCodes","fr,bm",
                "community",    "Dogon",
                "tags",         "art,masque,cérémonie"
        ));

        consumer.culture(raw);

        ArgumentCaptor<DiscoveryDocument> cap = ArgumentCaptor.forClass(DiscoveryDocument.class);
        verify(projections).project(cap.capture());

        DiscoveryDocument doc = cap.getValue();
        assertThat(doc.type()).isEqualTo(DiscoveryType.CULTURE);
        assertThat(doc.title()).isEqualTo("Masques Dogon");
        assertThat(doc.sourceId()).isEqualTo("culture:cnt-1");
        assertThat(doc.active()).isTrue();
        assertThat(doc.countryCode()).isEqualTo("ML");
        assertThat(doc.languageCodes()).isEqualTo("fr,bm");
        assertThat(doc.community()).isEqualTo("Dogon");
        assertThat(doc.tags()).isEqualTo("art,masque,cérémonie");
    }

    @Test
    void cultureContentPublished_language_type_maps_correctly() throws Exception {
        String raw = event("CultureContentPublished", Map.of(
                "contentId",   "lang-1",
                "title",       "Wolof",
                "status",      "PUBLISHED",
                "contentType", "LANGUAGE"
        ));

        consumer.culture(raw);

        ArgumentCaptor<DiscoveryDocument> cap = ArgumentCaptor.forClass(DiscoveryDocument.class);
        verify(projections).project(cap.capture());
        assertThat(cap.getValue().type()).isEqualTo(DiscoveryType.LANGUAGE);
    }

    // -------------------------------------------------------------------------
    // ArtworkPublished
    // -------------------------------------------------------------------------

    @Test
    void artworkPublished_projects_artwork_with_materials() throws Exception {
        String raw = event("ArtworkPublished", Map.of(
                "artworkId",           "art-1",
                "title",               "Bronze du Bénin",
                "status",              "PUBLISHED",
                "countryCode",         "BJ",
                "materials",           "bronze,cuivre",
                "techniques",          "fonte_perdue",
                "verificationStatus",  "VERIFIED",
                "availabilityStatus",  "AVAILABLE",
                "priceMin",            "500000",
                "priceMax",            "2000000"
        ));

        consumer.artisan(raw);

        ArgumentCaptor<DiscoveryDocument> cap = ArgumentCaptor.forClass(DiscoveryDocument.class);
        verify(projections).project(cap.capture());

        DiscoveryDocument doc = cap.getValue();
        assertThat(doc.type()).isEqualTo(DiscoveryType.ARTWORK);
        assertThat(doc.materials()).isEqualTo("bronze,cuivre");
        assertThat(doc.techniques()).isEqualTo("fonte_perdue");
        assertThat(doc.verificationStatus()).isEqualTo("VERIFIED");
        assertThat(doc.availabilityStatus()).isEqualTo("AVAILABLE");
        assertThat(doc.priceMin()).isNotNull();
        assertThat(doc.countryCode()).isEqualTo("BJ");
    }

    // -------------------------------------------------------------------------
    // ArtisanVerified
    // -------------------------------------------------------------------------

    @Test
    void artisanVerified_sets_verification_status() throws Exception {
        String raw = event("ArtisanVerified", Map.of(
                "artisanId",    "artisan-42",
                "displayName",  "Sékou Kouyaté",
                "countryCode",  "GN",
                "languageCodes","fr,sos"
        ));

        consumer.artisan(raw);

        ArgumentCaptor<DiscoveryDocument> cap = ArgumentCaptor.forClass(DiscoveryDocument.class);
        verify(projections).project(cap.capture());

        DiscoveryDocument doc = cap.getValue();
        assertThat(doc.type()).isEqualTo(DiscoveryType.ARTISAN);
        assertThat(doc.verificationStatus()).isEqualTo("VERIFIED");
        assertThat(doc.active()).isTrue();
    }

    // -------------------------------------------------------------------------
    // ArtworkAvailabilityChanged
    // -------------------------------------------------------------------------

    @Test
    void artworkAvailabilityChanged_updates_existing_document() throws Exception {
        // Arrange: existing document
        DiscoveryDocument existing = minimalArtwork("artwork:art-2", "AVAILABLE");
        when(projections.find("artwork:art-2")).thenReturn(Optional.of(existing));

        String raw = event("ArtworkAvailabilityChanged", Map.of(
                "artworkId",          "art-2",
                "availabilityStatus", "SOLD"
        ));

        consumer.artisan(raw);

        ArgumentCaptor<DiscoveryDocument> cap = ArgumentCaptor.forClass(DiscoveryDocument.class);
        verify(projections).project(cap.capture());
        assertThat(cap.getValue().availabilityStatus()).isEqualTo("SOLD");
    }

    @Test
    void artworkAvailabilityChanged_noop_when_document_not_found() throws Exception {
        when(projections.find(any())).thenReturn(Optional.empty());

        String raw = event("ArtworkAvailabilityChanged", Map.of(
                "artworkId",          "art-missing",
                "availabilityStatus", "SOLD"
        ));

        consumer.artisan(raw);

        verify(projections, never()).project(any());
    }

    // -------------------------------------------------------------------------
    // Deduplication
    // -------------------------------------------------------------------------

    @Test
    void duplicate_event_is_ignored() throws Exception {
        when(processed.existsById(any())).thenReturn(true);

        String raw = event("CultureContentPublished", Map.of(
                "contentId", "cnt-dup",
                "title",     "Dup",
                "status",    "PUBLISHED"
        ));

        consumer.culture(raw);
        verify(projections, never()).project(any());
    }

    // -------------------------------------------------------------------------
    // Contract validation
    // -------------------------------------------------------------------------

    @Test
    void wrong_event_version_throws() {
        String raw = """
                {"eventId":"%s","eventType":"CultureContentPublished","eventVersion":2,
                 "payload":{"contentId":"c1","title":"T","status":"PUBLISHED"}}"""
                .formatted(UUID.randomUUID());

        assertThatThrownBy(() -> consumer.culture(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported culture event version");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String event(String type, Map<String, Object> payload) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "eventId",      UUID.randomUUID().toString(),
                "eventType",    type,
                "eventVersion", 1,
                "occurredAt",   "2024-01-15T10:00:00Z",
                "payload",      payload
        ));
    }

    private DiscoveryDocument minimalArtwork(String sourceId, String availability) {
        return new DiscoveryDocument(
                UUID.nameUUIDFromBytes(sourceId.getBytes()),
                sourceId,
                DiscoveryType.ARTWORK,
                "Some Artwork",
                null, null, null, null, null, null, null,
                0, true, null, java.time.Instant.now(),
                null, null, null, null, null, null, null, null, null, null,
                "VERIFIED", availability, null, null, 0
        );
    }
}
