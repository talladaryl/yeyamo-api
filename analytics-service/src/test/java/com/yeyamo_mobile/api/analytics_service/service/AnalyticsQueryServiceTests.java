package com.yeyamo_mobile.api.analytics_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;
import com.yeyamo_mobile.api.analytics_service.repository.AnalyticsEventLogRepository;
import com.yeyamo_mobile.api.analytics_service.repository.KpiHistoryRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PartnerAnalyticsRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PlacePopularityRepository;
import com.yeyamo_mobile.api.analytics_service.repository.RegionActivityRepository;
import com.yeyamo_mobile.api.analytics_service.repository.UserEngagementRepository;

class AnalyticsQueryServiceTests {

    @Test
    void aggregatesAndRanksDailyPlacePopularityOverTheRequestedPeriod() {
        PlacePopularityRepository places = mock(PlacePopularityRepository.class);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PlacePopularity firstDay = place(first, 1, 1);
        PlacePopularity secondDay = place(first, 0, 2);
        PlacePopularity other = place(second, 1, 0);
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 15);
        when(places.findByStatDateBetween(from, to)).thenReturn(List.of(firstDay, secondDay, other));
        AnalyticsQueryService service = service(places);

        var result = service.popularPlaces(from, to, 20);

        assertEquals(first, result.getFirst().placeId());
        assertEquals(3, result.getFirst().likes());
        assertEquals(1, result.getFirst().checkIns());
    }

    @Test
    void rejectsAnExcessiveQueryRange() {
        AnalyticsQueryService service = service(mock(PlacePopularityRepository.class));

        assertThrows(IllegalArgumentException.class, () -> service.popularPlaces(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 7, 15), 20));
    }

    private AnalyticsQueryService service(PlacePopularityRepository places) {
        return new AnalyticsQueryService(mock(KpiHistoryRepository.class),
                mock(AnalyticsEventLogRepository.class), mock(RegionActivityRepository.class),
                mock(PartnerAnalyticsRepository.class), places, mock(UserEngagementRepository.class), 366);
    }

    private PlacePopularity place(UUID placeId, int checkIns, int likes) {
        PlacePopularity place = new PlacePopularity();
        place.setId(UUID.randomUUID());
        place.setPlaceId(placeId);
        place.setStatDate(LocalDate.of(2026, 7, 15));
        place.apply(UUID.randomUUID(), 0, likes, 0, 0, checkIns, 0, 0, Instant.now());
        return place;
    }
}
