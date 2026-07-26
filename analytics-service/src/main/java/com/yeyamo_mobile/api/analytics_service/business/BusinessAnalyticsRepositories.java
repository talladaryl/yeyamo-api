package com.yeyamo_mobile.api.analytics_service.business;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

interface AnalyticsInboxRepository extends JpaRepository<AnalyticsInbox, UUID> {}

interface StoredAnalyticsEventRepository
        extends JpaRepository<StoredAnalyticsEvent, UUID> {
    List<StoredAnalyticsEvent> findAllByOrderByOccurredAtAscEventIdAsc();
}

interface ReachFingerprintRepository
        extends JpaRepository<ReachFingerprint, String> {}

interface DailyAggregateRepository extends JpaRepository<DailyAggregate, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a from DailyAggregate a where a.statDate=:date
        and a.scopeType=:scopeType and a.scopeId=:scopeId
        and a.dimensionType=:dimensionType and a.dimensionValue=:dimensionValue
        """)
    Optional<DailyAggregate> locked(
        @Param("date") LocalDate date,
        @Param("scopeType") String scopeType,
        @Param("scopeId") String scopeId,
        @Param("dimensionType") String dimensionType,
        @Param("dimensionValue") String dimensionValue);

    Page<DailyAggregate> findByScopeTypeAndScopeIdAndStatDateBetween(
        String scopeType, String scopeId, LocalDate from, LocalDate to,
        Pageable pageable);

    Page<DailyAggregate> findByPartnerIdAndScopeTypeAndScopeIdAndStatDateBetween(
        String partnerId, String scopeType, String scopeId,
        LocalDate from, LocalDate to, Pageable pageable);

    Optional<DailyAggregate>
    findFirstByPartnerIdAndScopeTypeAndScopeIdAndDimensionTypeAndStatDateBetweenOrderByScansDesc(
        String partnerId, String scopeType, String scopeId, String dimensionType,
        LocalDate from, LocalDate to);

    @Modifying
    @Query("delete from DailyAggregate")
    void clearAll();
}
