package com.yeyamo_mobile.api.analytics_service.repository;

import com.yeyamo_mobile.api.analytics_service.domain.model.ArtisanKpisDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ArtisanKpisDailyRepository
        extends JpaRepository<ArtisanKpisDaily, String> {

    Optional<ArtisanKpisDaily> findByAggregationDateAndArtisanId(
            LocalDate date, String artisanId);

    List<ArtisanKpisDaily> findByArtisanIdAndAggregationDateBetweenOrderByAggregationDateDesc(
            String artisanId, LocalDate from, LocalDate to);

    @Query("""
            SELECT SUM(k.sales) FROM ArtisanKpisDaily k
            WHERE k.artisanId = :artisanId
              AND k.aggregationDate BETWEEN :from AND :to
            """)
    Long totalSalesForArtisan(@Param("artisanId") String artisanId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
