package com.yeyamo_mobile.api.analytics_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.service.AnalyticsIngestionService;
import com.yeyamo_mobile.api.analytics_service.business.BusinessAnalyticsService;

@Component
public class AnalyticsEventConsumer {

    private final AnalyticsIngestionService ingestionService;
    private final BusinessAnalyticsService businessAnalytics;

    public AnalyticsEventConsumer(AnalyticsIngestionService ingestionService,
            BusinessAnalyticsService businessAnalytics) {
        this.ingestionService = ingestionService;
        this.businessAnalytics = businessAnalytics;
    }

    @KafkaListener(
            topics = "#{'${yeyamo.kafka.consumer.topics:user-events,partner-events,place-events,social-events,graph-events,booking-events,audit-events}'.split(',')}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service}"
    )
    public void consume(String payload) {
        businessAnalytics.ingest(payload);
        ingestionService.process(payload);
    }
}
