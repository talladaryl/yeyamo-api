package com.yeyamo_mobile.api.booking_service.infrastructure.outbox;

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
class BookingPaymentCommandKafkaTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void publishesPaymentCommandWithMobileMoneyFields() throws Exception {
        BookingOutboxRepository repository = mock(BookingOutboxRepository.class);
        BookingOutboxEvent event = new BookingOutboxEvent();
        event.id = UUID.randomUUID();
        event.aggregateId = "booking-1";
        event.targetTopic = "payment.commands";
        event.payload = """
                {"eventType":"payment.authorization.requested","payload":{"operator":"mtn","phoneNumber":"+237690123456","country":"CM"}}
                """;
        when(repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()).thenReturn(List.of(event));

        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(broker);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
        KafkaTemplate<String, String> kafka = new KafkaTemplate<>(producerFactory);
        BookingOutboxPublisher publisher = new BookingOutboxPublisher(repository, kafka);

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("booking-mobile-money", "false", broker);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProperties).createConsumer()) {
            TopicPartition paymentCommands = new TopicPartition("payment.commands", 0);
            consumer.assign(List.of(paymentCommands));
            consumer.seekToBeginning(List.of(paymentCommands));
            publisher.publish();

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "payment.commands");
            JsonNode payload = new ObjectMapper().readTree(record.value()).path("payload");
            assertEquals("mtn", payload.path("operator").asText());
            assertEquals("+237690123456", payload.path("phoneNumber").asText());
            assertEquals("CM", payload.path("country").asText());
        } finally {
            producerFactory.destroy();
        }
    }
}
