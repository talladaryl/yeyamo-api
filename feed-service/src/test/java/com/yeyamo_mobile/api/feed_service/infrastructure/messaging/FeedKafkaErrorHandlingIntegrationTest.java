package com.yeyamo_mobile.api.feed_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import com.yeyamo_mobile.api.feed_service.infrastructure.persistence.SpringFeedPostRepository;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.listener.ack-mode=record",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "feed.outbox.enabled=false",
        "spring.data.redis.repositories.enabled=false",
        "management.health.redis.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration"
})
@EmbeddedKafka(partitions = 1, topics = { "content.events", "content.events.DLT" },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class FeedKafkaErrorHandlingIntegrationTest {

    @Autowired private KafkaTemplate<String, String> kafka;
    @Autowired private KafkaListenerEndpointRegistry listenerRegistry;
    @Autowired private SpringFeedPostRepository posts;
    @Value("${spring.kafka.bootstrap-servers}") private String bootstrapServers;

    @Test
    void malformedRawJsonIsSentToDltAndTheNextValidRecordIsProcessed() throws Exception {
        MessageListenerContainer contentListener = listenerRegistry.getListenerContainers().stream()
                .filter(container -> List.of(container.getContainerProperties().getTopics()).contains("content.events"))
                .findFirst()
                .orElseThrow();
        ContainerTestUtils.waitForAssignment(contentListener, 1);

        try (Consumer<String, String> dltConsumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "feed-dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            dltConsumer.subscribe(List.of("content.events.DLT"));
            dltConsumer.poll(Duration.ofMillis(100));

            kafka.send("content.events", "malformed-json").get();

            ConsumerRecord<String, String> dltRecord = awaitDltRecord(dltConsumer, Duration.ofSeconds(12));
            assertNotNull(dltRecord);
            assertEquals("malformed-json", dltRecord.value());

            UUID eventId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            kafka.send("content.events", validPost(eventId, postId)).get();

            await(Duration.ofSeconds(12), () -> posts.existsById(postId));
            assertTrue(posts.existsById(postId));
        }
    }

    private static ConsumerRecord<String, String> awaitDltRecord(Consumer<String, String> consumer, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(250))) {
                return record;
            }
        }
        return null;
    }

    private static String validPost(UUID eventId, UUID postId) {
        return """
                {"eventId":"%s","eventType":"content.post.published","eventVersion":1,
                "producer":"content-service","occurredAt":"2026-09-23T00:00:00Z",
                "payload":{"postId":"%s","authorId":"author-1","status":"PUBLISHED",
                "visibility":"PUBLIC","referenceType":"NONE","mediaIds":[],"hashtags":[]}}
                """.formatted(eventId, postId);
    }

    private static void await(Duration timeout, Check check) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (check.value()) return;
            Thread.sleep(50);
        }
        assertTrue(check.value(), "Expected Feed projection was not processed before timeout");
    }

    @FunctionalInterface
    private interface Check {
        boolean value() throws Exception;
    }
}
