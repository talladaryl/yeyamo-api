package com.yeyamo_mobile.api.analytics_service.repository;

import com.yeyamo_mobile.api.analytics_service.domain.model.CultureEngagementDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CultureEngagementDailyRepository
        extends JpaRepository<CultureEngagementDaily, String> {

    Optional<CultureEngagementDaily> findByAggregationDateAndCountryCodeAndCultureIdAndLanguageCode(
            LocalDate date, String countryCode, String cultureId, String languageCode);

    List<CultureEngagementDaily> findByLanguageCodeAndAggregationDateBetween(
            String languageCode, LocalDate from, LocalDate to);

    List<CultureEngagementDaily> findByCountryCodeAndAggregationDateBetween(
            String countryCode, LocalDate from, LocalDate to);

    @Query("""
            SELECT e FROM CultureEngagementDaily e
            WHERE e.aggregationDate BETWEEN :from AND :to
            ORDER BY e.contentViews DESC
            """)
    List<CultureEngagementDaily> findTopByDateRange(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT SUM(e.contentViews) FROM CultureEngagementDaily e
            WHERE e.aggregationDate BETWEEN :from AND :to
              AND (:countryCode IS NULL OR e.countryCode = :countryCode)
            """)
    Long sumContentViews(@Param("from") LocalDate from,
                         @Param("to") LocalDate to,
                         @Param("countryCode") String countryCode);
}
