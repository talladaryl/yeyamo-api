package com.yeyamo_mobile.api.analytics_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.yeyamo_mobile.api.analytics_service.models.PartnerAnalytics;

public interface PartnerAnalyticsRepository extends ElasticsearchRepository<PartnerAnalytics, UUID> {
    List<PartnerAnalytics> findByPartnerIdOrderByStatDateDesc(UUID partnerId);
}
