package com.yeyamo_mobile.api.feed_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.feed_service.application.FeedProjectionCommandService;

class FeedEventConsumerRobustnessTests {

    @Test
    void validCultureEventOutsideTheFeedContractIsAcknowledgedWithoutProjection() {
        UUID eventId = UUID.randomUUID();
        FeedProjectionCommandService commands = mock(FeedProjectionCommandService.class);
        ProcessedEventRepository receipts = mock(ProcessedEventRepository.class);
        when(receipts.existsById(eventId)).thenReturn(false);
        FeedEventConsumer consumer = new FeedEventConsumer(new ObjectMapper(), commands, receipts);

        String raw = """
                {"eventId":"%s","eventType":"CultureTranslationAdded","eventVersion":1,
                "producer":"culture-service","payload":{"contentId":"%s","languageCode":"fr"}}
                """.formatted(eventId, UUID.randomUUID());

        assertDoesNotThrow(() -> consumer.culture(raw));

        verifyNoInteractions(commands);
        verify(receipts).save(any());
    }
}
