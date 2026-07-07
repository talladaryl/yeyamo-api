package com.yeyamo_mobile.api.analytics_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.service.AnalyticsIngestionService;

@Component
public class AnalyticsEventConsumer {

    private final AnalyticsIngestionService ingestionService;

    public AnalyticsEventConsumer(AnalyticsIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @KafkaListener(
            topics = "#{'${yeyamo.kafka.consumer.topics:user-events,partner-events,place-events,social-events,graph-events,booking-events,audit-events}'.split(',')}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service}"
    )
    public void consume(String payload) {
        ingestionService.process(payload);
    }
}
