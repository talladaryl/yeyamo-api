package com.yeyamo_mobile.api.analytics_service.repository;

import com.yeyamo_mobile.api.analytics_service.domain.model.LanguageLearningDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LanguageLearningDailyRepository
        extends JpaRepository<LanguageLearningDaily, String> {

    Optional<LanguageLearningDaily> findByAggregationDateAndLanguageCodeAndCountryCode(
            LocalDate date, String languageCode, String countryCode);

    List<LanguageLearningDaily> findByLanguageCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
            String languageCode, LocalDate from, LocalDate to);

    List<LanguageLearningDaily> findByCountryCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
            String countryCode, LocalDate from, LocalDate to);

    @Query("""
            SELECT l.languageCode, SUM(l.activeLearners) as total
            FROM LanguageLearningDaily l
            WHERE l.aggregationDate BETWEEN :from AND :to
            GROUP BY l.languageCode
            ORDER BY total DESC
            """)
    List<Object[]> topLanguagesByLearners(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT SUM(l.wordsLearned) FROM LanguageLearningDaily l
            WHERE l.languageCode = :code
              AND l.aggregationDate BETWEEN :from AND :to
            """)
    Long totalWordsLearnedForLanguage(
            @Param("code") String code,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
