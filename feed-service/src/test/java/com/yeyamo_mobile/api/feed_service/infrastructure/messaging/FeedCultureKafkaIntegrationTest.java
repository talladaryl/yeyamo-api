package com.yeyamo_mobile.api.feed_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.context.EmbeddedKafka;
import com.yeyamo_mobile.api.feed_service.infrastructure.persistence.SpringCultureContentReadModelRepository;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=true",
        "feed.outbox.enabled=false",
        "spring.data.redis.repositories.enabled=false",
        "management.health.redis.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = "culture.events", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class FeedCultureKafkaIntegrationTest {
    @Autowired private KafkaTemplate<String, String> kafka;
    @Autowired private SpringCultureContentReadModelRepository cultureContents;
    @Autowired private KafkaListenerEndpointRegistry listenerRegistry;

    @Test
    void projectsAndDeactivatesCultureContentFromKafka() throws Exception {
        UUID contentId = UUID.randomUUID();
        ContainerTestUtils.waitForAssignment(
                listenerRegistry.getListenerContainer("feed-culture-events"), 1);
        kafka.send("culture.events", createdEvent(UUID.randomUUID(), contentId, true)).get();

        await(Duration.ofSeconds(20), () -> cultureContents.findById(contentId)
                .filter(entity -> entity.isActive() && "RECIPE".equals(entity.getType()))
                .isPresent());
        assertEquals("Ndolé", cultureContents.findById(contentId).orElseThrow().getTitle());

        kafka.send("culture.events", createdEvent(UUID.randomUUID(), contentId, false)).get();
        await(Duration.ofSeconds(20), () -> cultureContents.findById(contentId)
                .map(entity -> !entity.isActive())
                .orElse(false));
        assertFalse(cultureContents.findById(contentId).orElseThrow().isActive());
    }

    private static String createdEvent(UUID eventId, UUID contentId, boolean active) {
        return """
                {"eventId":"%s","eventType":"CultureContentPublished","eventVersion":1,
                "producer":"culture-service","correlationId":"%s",
                "payload":{"contentId":"%s","type":"RECIPE","title":"Ndolé","isActive":%s}}
                """.formatted(eventId, eventId, contentId, active);
    }

    private static void await(Duration timeout, Check check) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (check.value()) return;
            Thread.sleep(50);
        }
        assertTrue(check.value(), "La projection Kafka n’a pas été enregistrée avant expiration du délai.");
    }

    @FunctionalInterface
    private interface Check { boolean value() throws Exception; }
}
