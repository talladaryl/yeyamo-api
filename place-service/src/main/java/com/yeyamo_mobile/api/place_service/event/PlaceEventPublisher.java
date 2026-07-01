package com.yeyamo_mobile.api.place_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.place_service.models.Place;

@Component
public class PlaceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PlaceEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public PlaceEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${yeyamo.kafka.topics.place-events:place.events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publishCreated(Place place) {
        publish(PlaceEvent.created(toPayload(place)));
    }

    public void publishUpdated(Place place) {
        publish(PlaceEvent.updated(toPayload(place)));
    }

    private PlaceEventPayload toPayload(Place place) {
        return new PlaceEventPayload(
                place.getId(),
                place.getPartnerId(),
                place.getName(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory() != null ? place.getCategory().getSlug() : null
        );
    }

    private void publish(PlaceEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.payload().placeId().toString(), payload);
        } catch (JsonProcessingException exception) {
            log.error("Impossible de serialiser l'evenement Kafka pour le lieu {}", event.payload().placeId(), exception);
        }
    }
}
