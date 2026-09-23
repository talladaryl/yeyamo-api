package com.yeyamo_mobile.api.place_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionModerationRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionRequest;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.event.PlaceEventPublisher;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.Place;
import com.yeyamo_mobile.api.place_service.models.PlaceSuggestion;
import com.yeyamo_mobile.api.place_service.repository.PlaceRepository;
import com.yeyamo_mobile.api.place_service.repository.PlaceSuggestionRepository;
import com.yeyamo_mobile.shared.country.CountryConfigClient;

@ExtendWith(MockitoExtension.class)
class PlaceSuggestionServiceTest {
    @Mock PlaceSuggestionRepository suggestions;
    @Mock PlaceRepository places;
    @Mock PlaceService placesService;
    @Mock CountryConfigClient countries;
    @Mock SuggestionMediaVerifier media;
    @Mock PlaceEventPublisher events;
    private PlaceSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new PlaceSuggestionService(suggestions, places, placesService, countries, media, events, 8);
    }

    @Test
    void createsPendingSuggestionAndWritesCreatedOutboxEvent() {
        when(places.findNearbyDuplicateCandidates(any(Double.class), any(Double.class), any(Double.class))).thenReturn(List.of());
        when(suggestions.findDuplicateCandidates(eq(PlaceSuggestion.Status.PENDING), eq("CM"), any(), any()))
                .thenReturn(List.of());
        when(suggestions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(request(), "user-a");

        assertEquals("PENDING", response.status());
        assertEquals("CM", response.countryCode());
        verify(events).publishSuggestionCreated(any(PlaceSuggestion.class));
    }

    @Test
    void rejectsCertainPendingDuplicateAtCreateTime() {
        PlaceSuggestion existing = PlaceSuggestion.pending("user-b", "Musee National", "musee national", "Rue 1",
                "rue 1", null, null, null, null, "CM", "legacy-key-1", null, null, null, 3.866, 11.517);
        when(places.findNearbyDuplicateCandidates(any(Double.class), any(Double.class), any(Double.class))).thenReturn(List.of());
        when(suggestions.findDuplicateCandidates(eq(PlaceSuggestion.Status.PENDING), eq("CM"), any(), any()))
                .thenReturn(List.of(existing));

        ApiException error = assertThrows(ApiException.class, () -> service.create(request(), "user-a"));

        assertEquals("CERTAIN_DUPLICATE", error.getCode());
        verify(suggestions, never()).saveAndFlush(any());
    }

    @Test
    void secondApproveDoesNotCreateAnotherCanonicalPlaceOrEvent() {
        PlaceSuggestion pending = PlaceSuggestion.pending("user-a", "Musee National", "musee national", "Rue 1",
                "rue 1", null, null, null, null, "CM", "legacy-key-2", null, null, null, 3.866, 11.517);
        Place canonical = new Place();
        canonical.setId(UUID.randomUUID());
        canonical.setName("Musee National");
        canonical.setAddress("Rue 1");
        canonical.setLatitude(3.866);
        canonical.setLongitude(11.517);
        when(suggestions.findLockedById(any())).thenReturn(Optional.of(pending));
        when(places.findNearbyDuplicateCandidates(any(Double.class), any(Double.class), any(Double.class)))
                .thenReturn(List.of(canonical));
        when(suggestions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var first = service.approve(UUID.randomUUID(), moderation(), "moderator-a");
        var second = service.approve(UUID.randomUUID(), moderation(), "moderator-b");

        assertEquals("APPROVED", first.status());
        assertEquals(first.canonicalPlaceId(), second.canonicalPlaceId());
        verify(placesService, never()).create(any());
        verify(events).publishSuggestionApproved(any(PlaceSuggestion.class));
    }

    private PlaceSuggestionRequest request() {
        return new PlaceSuggestionRequest("Musee National", "Rue 1", null, null, null, null, "CM", null, null,
                null, List.of(), 3.866, 11.517);
    }

    private PlaceSuggestionModerationRequest moderation() {
        return new PlaceSuggestionModerationRequest(1L, 1L, UUID.randomUUID(), null, null, PlaceStatus.PUBLISHED, null);
    }
}
