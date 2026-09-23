package com.yeyamo_mobile.api.feed_service.infrastructure.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Compatibility listener for the user-service's established user-events topic. */
@Component
public class LegacyUserSocialEventConsumer {
    private final FeedEventConsumer delegate;
    public LegacyUserSocialEventConsumer(FeedEventConsumer delegate) { this.delegate = delegate; }
    @KafkaListener(topics = "${yeyamo.kafka.topics.legacy-user-events:user-events}",
            groupId = "${spring.kafka.consumer.group-id:feed-service}-social-legacy")
    @Transactional
    public void consume(String raw) throws Exception { delegate.user(raw); }
}
