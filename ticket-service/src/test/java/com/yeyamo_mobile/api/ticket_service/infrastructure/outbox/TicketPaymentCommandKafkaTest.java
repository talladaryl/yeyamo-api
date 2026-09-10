package com.yeyamo_mobile.api.ticket_service.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@EmbeddedKafka(partitions = 1, topics = "payment.commands")
class TicketPaymentCommandKafkaTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void publishesPaymentCommandWithMobileMoneyFields() throws Exception {
        SpringOutboxRepository repository = mock(SpringOutboxRepository.class);
        TicketOutboxEntity event = new TicketOutboxEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateId("order-1");
        event.setEventType("payment.authorization.requested");
        event.setPayload("""
                {"eventType":"payment.authorization.requested","payload":{"operator":"orange","phoneNumber":"+2250701234567","country":"CI"}}
                """);
        when(repository.findPendingEventsBatch()).thenReturn(List.of(event));

        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(broker);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
        KafkaTemplate<String, String> kafka = new KafkaTemplate<>(producerFactory);
        OutboxPublisher publisher = new OutboxPublisher(repository, kafka);

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(broker, "ticket-mobile-money", false);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProperties).createConsumer()) {
            TopicPartition paymentCommands = new TopicPartition("payment.commands", 0);
            consumer.assign(List.of(paymentCommands));
            consumer.seekToBeginning(List.of(paymentCommands));
            publisher.publishPendingEvents();

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "payment.commands");
            JsonNode payload = new ObjectMapper().readTree(record.value()).path("payload");
            assertEquals("orange", payload.path("operator").asText());
            assertEquals("+2250701234567", payload.path("phoneNumber").asText());
            assertEquals("CI", payload.path("country").asText());
        } finally {
            producerFactory.destroy();
        }
    }
}
