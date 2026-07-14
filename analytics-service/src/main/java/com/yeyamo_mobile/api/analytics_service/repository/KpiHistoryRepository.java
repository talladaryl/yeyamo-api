package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;

public interface KpiHistoryRepository extends ElasticsearchRepository<KpiHistory, UUID> {
    List<KpiHistory> findByKpiNameOrderByCalculatedAtDesc(String kpiName);
    List<KpiHistory> findTop20ByOrderByCalculatedAtDesc();
}
