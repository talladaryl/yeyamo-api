package com.yeyamo_mobile.api.discovery_service.infrastructure.postgis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.yeyamo_mobile.api.discovery_service.application.DiscoveryScope;
import com.yeyamo_mobile.api.discovery_service.application.DiscoverySearch;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;

class PostgisDiscoverySearchAdapterTest {
    private SpringDiscoveryDocumentRepository documents;
    private PendingTrendRepository pending;
    private PostgisDiscoverySearchAdapter adapter;

    @BeforeEach
    void setUp() {
        documents = org.mockito.Mockito.mock(SpringDiscoveryDocumentRepository.class);
        pending = org.mockito.Mockito.mock(PendingTrendRepository.class);
        when(pending.findById(any())).thenReturn(Optional.empty());
        adapter = new PostgisDiscoverySearchAdapter(documents, pending);
    }

    @Test
    void upsertPersistsCultureProjectionWithoutDroppingFields() {
        DiscoveryDocument document = document();
        when(documents.findBySourceId(document.sourceId())).thenReturn(Optional.empty());

        adapter.upsert(document);

        ArgumentCaptor<DiscoveryDocumentEntity> captor = ArgumentCaptor.forClass(DiscoveryDocumentEntity.class);
        verify(documents).save(captor.capture());
        DiscoveryDocumentEntity saved = captor.getValue();
        assertThat(saved.getCountryCode()).isEqualTo("ML");
        assertThat(saved.getLanguageCodes()).isEqualTo("fr,bm");
        assertThat(saved.getMaterials()).isEqualTo("bronze");
        assertThat(saved.getTechniques()).isEqualTo("forge");
        assertThat(saved.getAvailabilityStatus()).isEqualTo("AVAILABLE");
        assertThat(saved.getLaunchVisibility()).isEqualTo("PUBLIC");
    }

    @Test
    void searchPassesCultureAndTerritoryFiltersToPostgisRepository() {
        DiscoverySearch criteria = new DiscoverySearch(
                "masque", DiscoveryType.ARTWORK, "art", "BKO", 12.0, -8.0, 10.0,
                1, 20, false, "ML", "bamako", "city-1", "bm", "ARTWORK",
                "bronze", "forge", true, true, java.util.Set.of("ML", "SN"), DiscoveryScope.COUNTRY);
        when(documents.searchGeo(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(Boolean.class), any(Double.class), any(Double.class), any(Double.class), anyInt(), anyInt()))
                .thenReturn(List.of());

        adapter.search(criteria, 21);

        verify(documents).searchGeo(
                "masque", "ARTWORK", "art", "BKO", "ML", "ML,SN", "bamako", "city-1", "bm", "ARTWORK",
                "bronze", "forge", true, true, false, 12.0, -8.0, 10000.0, 21, 20);
    }

    private DiscoveryDocument document() {
        Instant now = Instant.now();
        return new DiscoveryDocument(UUID.randomUUID(), "artwork:1", DiscoveryType.ARTWORK, "Masque", "Description",
                "art", "BKO", "Bamako", 12.0, -8.0, "artisan-1", 2.0, true, now, now,
                "ML", "bamako", "city-1", "{\"fr\":\"Masque\"}", "fr,bm", "community", "tag",
                "bronze", "forge", "artisan-1", "VERIFIED", "AVAILABLE", BigDecimal.ONE, BigDecimal.TEN,
                4.0, "PUBLIC", "mask");
    }
}
