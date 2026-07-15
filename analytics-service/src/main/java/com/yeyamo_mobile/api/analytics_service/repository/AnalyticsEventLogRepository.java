package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;

public interface AnalyticsEventLogRepository extends ElasticsearchRepository<AnalyticsEventLog, UUID> {
    List<AnalyticsEventLog> findTop50ByOrderByProcessedAtDesc();
    boolean existsByEventId(UUID eventId);
    Optional<AnalyticsEventLog> findByEventId(UUID eventId);
}
