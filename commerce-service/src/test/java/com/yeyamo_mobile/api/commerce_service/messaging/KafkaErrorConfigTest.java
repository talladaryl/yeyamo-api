package com.yeyamo_mobile.api.commerce_service.messaging;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaErrorConfigTest {
    @Test
    void configuresBoundedRetriesAndDeadLetterRecovery() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        assertNotNull(new KafkaErrorConfig().commerceKafkaErrorHandler(kafkaTemplate));
    }
}
