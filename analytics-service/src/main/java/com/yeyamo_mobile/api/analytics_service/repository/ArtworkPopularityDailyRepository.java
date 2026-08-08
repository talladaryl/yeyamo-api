package com.yeyamo_mobile.api.analytics_service.repository;

import com.yeyamo_mobile.api.analytics_service.domain.model.ArtworkPopularityDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ArtworkPopularityDailyRepository
        extends JpaRepository<ArtworkPopularityDaily, String> {

    Optional<ArtworkPopularityDaily> findByAggregationDateAndArtworkId(
            LocalDate date, String artworkId);

    List<ArtworkPopularityDaily> findByArtworkIdAndAggregationDateBetweenOrderByAggregationDateDesc(
            String artworkId, LocalDate from, LocalDate to);

    List<ArtworkPopularityDaily> findByArtisanIdAndAggregationDateBetweenOrderByAggregationDateDesc(
            String artisanId, LocalDate from, LocalDate to);

    @Query("""
            SELECT a FROM ArtworkPopularityDaily a
            WHERE a.aggregationDate BETWEEN :from AND :to
            ORDER BY a.views DESC
            """)
    List<ArtworkPopularityDaily> findTopByDateRange(
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
