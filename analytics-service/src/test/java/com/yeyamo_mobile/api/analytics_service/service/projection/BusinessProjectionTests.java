package com.yeyamo_mobile.api.analytics_service.service.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;
import com.yeyamo_mobile.api.analytics_service.models.UserEngagement;
import com.yeyamo_mobile.api.analytics_service.repository.ContentDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PlacePopularityRepository;
import com.yeyamo_mobile.api.analytics_service.repository.UserEngagementRepository;

class BusinessProjectionTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void publishedContentIncrementsTheAuthorsDailyEngagement() throws Exception {
        UserEngagementRepository engagements = mock(UserEngagementRepository.class);
        ContentDimensionRepository contents = mock(ContentDimensionRepository.class);
        when(engagements.findById(any())).thenReturn(Optional.empty());
        var projection = new UserEngagementProjection(engagements, contents);
        AnalyticsDomainEvent event = event("content.post.published",
                "{\"authorId\":\"user-1\",\"publishedAt\":\"2026-07-15T10:00:00Z\"}");

        projection.project(event);

        ArgumentCaptor<UserEngagement> saved = ArgumentCaptor.forClass(UserEngagement.class);
        verify(engagements).save(saved.capture());
        assertEquals("user-1", saved.getValue().getUserId());
        assertEquals(1, saved.getValue().getPostsCount());
        assertEquals(1, saved.getValue().getAppliedEventIds().size());
    }

    @Test
    void aCheckInContributesSixPopularityPoints() throws Exception {
        PlacePopularityRepository places = mock(PlacePopularityRepository.class);
        when(places.findById(any())).thenReturn(Optional.empty());
        var projection = new PlacePopularityProjection(places, mock(ContentDimensionRepository.class));
        UUID placeId = UUID.randomUUID();

        projection.project(event("interaction.checkin.created", "{\"catalogAssetId\":\"" + placeId + "\"}"));

        ArgumentCaptor<PlacePopularity> saved = ArgumentCaptor.forClass(PlacePopularity.class);
        verify(places).save(saved.capture());
        assertEquals(1, saved.getValue().getCheckIns());
        assertEquals(0, saved.getValue().getPopularityScore().compareTo(java.math.BigDecimal.valueOf(6)));
    }

    private AnalyticsDomainEvent event(String type, String payload) throws Exception {
        return new AnalyticsDomainEvent(UUID.randomUUID(), type, 1,
                Instant.parse("2026-07-15T10:00:00Z"), "test-service", "corr-1", "user-1",
                mapper.readTree(payload));
    }
}
