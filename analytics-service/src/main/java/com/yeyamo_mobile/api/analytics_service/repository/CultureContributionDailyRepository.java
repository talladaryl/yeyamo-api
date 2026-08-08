package com.yeyamo_mobile.api.analytics_service.repository;

import com.yeyamo_mobile.api.analytics_service.domain.model.CultureContributionDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CultureContributionDailyRepository
        extends JpaRepository<CultureContributionDaily, String> {

    Optional<CultureContributionDaily> findByAggregationDateAndCountryCodeAndContributionType(
            LocalDate date, String countryCode, String contributionType);

    List<CultureContributionDaily> findByAggregationDateBetweenOrderByAggregationDateDesc(
            LocalDate from, LocalDate to);

    List<CultureContributionDaily> findByCountryCodeAndAggregationDateBetween(
            String countryCode, LocalDate from, LocalDate to);

    @Query("""
            SELECT c.contributionType, SUM(c.totalContributions)
            FROM CultureContributionDaily c
            WHERE c.aggregationDate BETWEEN :from AND :to
              AND (:country IS NULL OR c.countryCode = :country)
            GROUP BY c.contributionType
            """)
    List<Object[]> sumByTypeAndDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("country") String countryCode);
}
