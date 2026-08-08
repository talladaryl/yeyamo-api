package com.yeyamo_mobile.api.discovery_service.application;

import com.yeyamo_mobile.api.discovery_service.application.port.DiscoveryCachePort;
import com.yeyamo_mobile.api.discovery_service.application.port.DiscoverySearchPort;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;
import com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin.SearchAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class CultureDiscoverySearchTest {

    private DiscoverySearchPort searchPort;
    private DiscoveryCachePort  cachePort;
    private DiscoveryQueryService queryService;

    @BeforeEach
    void setUp() {
        searchPort   = mock(DiscoverySearchPort.class);
        cachePort    = mock(DiscoveryCachePort.class);
        queryService = new DiscoveryQueryService(
                searchPort, cachePort,
                mock(com.yeyamo_mobile.api.discovery_service.application.AdInjectionService.class),
                mock(SearchAdminService.class));

        when(cachePort.get(any())).thenReturn(Optional.empty());
        doNothing().when(cachePort).put(any(), any());
    }

    // -------------------------------------------------------------------------
    // New filter parameters are passed through
    // -------------------------------------------------------------------------

    @Test
    void search_with_countryCode_passes_filter_to_port() {
        when(searchPort.search(any(), anyInt())).thenReturn(List.of());

        DiscoverySearch criteria = new DiscoverySearch(
                "masque", DiscoveryType.ARTWORK, null, null,
                null, null, null, 0, 10, false,
                "ML",    // countryCode
                null, null, null, null, null, null, null, null);

        queryService.search(criteria);

        verify(searchPort).search(argThat(s -> "ML".equals(s.countryCode())), anyInt());
    }

    @Test
    void search_with_languageCode_passes_filter() {
        when(searchPort.search(any(), anyInt())).thenReturn(List.of());

        DiscoverySearch criteria = new DiscoverySearch(
                null, DiscoveryType.LANGUAGE, null, null,
                null, null, null, 0, 10, false,
                null, null, null,
                "bm",    // languageCode — bambara
                null, null, null, null, null);

        queryService.search(criteria);

        verify(searchPort).search(argThat(s -> "bm".equals(s.languageCode())), anyInt());
    }

    @Test
    void search_with_verified_true_passes_filter() {
        when(searchPort.search(any(), anyInt())).thenReturn(List.of());

        DiscoverySearch criteria = new DiscoverySearch(
                null, DiscoveryType.ARTWORK, null, null,
                null, null, null, 0, 10, false,
                null, null, null, null, null, null, null, null,
                true);   // verified

        queryService.search(criteria);

        verify(searchPort).search(argThat(s -> Boolean.TRUE.equals(s.verified())), anyInt());
    }

    @Test
    void search_with_availability_false_passes_filter() {
        when(searchPort.search(any(), anyInt())).thenReturn(List.of());

        DiscoverySearch criteria = new DiscoverySearch(
                null, DiscoveryType.ARTWORK, null, null,
                null, null, null, 0, 10, false,
                null, null, null, null, null, null, null,
                false,   // availability = sold/unavailable
                null);

        queryService.search(criteria);

        verify(searchPort).search(argThat(s -> Boolean.FALSE.equals(s.availability())), anyInt());
    }

    // -------------------------------------------------------------------------
    // Results are returned correctly
    // -------------------------------------------------------------------------

    @Test
    void culture_documents_returned_by_search() {
        List<DiscoveryDocument> docs = List.of(
                artworkDoc("art-1", "ML"),
                artworkDoc("art-2", "SN")
        );
        when(searchPort.search(any(), anyInt())).thenReturn(docs);

        DiscoverySearch criteria = new DiscoverySearch(
                null, DiscoveryType.ARTWORK, null, null,
                null, null, null, 0, 10, false,
                null, null, null, null, null, null, null, null, null);

        DiscoveryPage page = queryService.search(criteria);

        assertThat(page.items()).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    void invalid_coordinates_throw() {
        assertThatThrownBy(() -> new DiscoverySearch(
                null, null, null, null,
                91.0, 0.0,   // invalid lat
                null, 0, 10, false,
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lat_without_lng_throws() {
        assertThatThrownBy(() -> new DiscoverySearch(
                null, null, null, null,
                1.0, null,   // lng missing
                null, 0, 10, false,
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void radius_above_200_throws() {
        assertThatThrownBy(() -> new DiscoverySearch(
                null, null, null, null,
                0.0, 0.0, 201.0,
                0, 10, false,
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // Backwards compatibility — old constructor still works
    // -------------------------------------------------------------------------

    @Test
    void legacy_constructor_works_without_culture_params() {
        when(searchPort.search(any(), anyInt())).thenReturn(List.of());

        DiscoverySearch criteria = new DiscoverySearch(
                "test", null, null, null, null, null, null, 0, 10, false);

        queryService.search(criteria);
        verify(searchPort).search(argThat(s ->
                s.query().equals("test") && s.countryCode() == null), anyInt());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DiscoveryDocument artworkDoc(String id, String country) {
        return new DiscoveryDocument(
                UUID.randomUUID(), "artwork:" + id, DiscoveryType.ARTWORK,
                "Artwork " + id, null, null, null, null, null, null, null,
                0, true, Instant.now(), Instant.now(),
                country, null, null, null, null, null, null,
                "bronze", "fonte_perdue", "artisan-1",
                "VERIFIED", "AVAILABLE",
                BigDecimal.valueOf(100000), BigDecimal.valueOf(500000),
                50.0);
    }
}
