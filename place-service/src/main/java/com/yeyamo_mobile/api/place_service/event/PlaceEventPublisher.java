package com.yeyamo_mobile.api.place_service.event;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.place_service.models.Place;
import com.yeyamo_mobile.api.place_service.outbox.PlaceOutboxMessage;
import com.yeyamo_mobile.api.place_service.outbox.PlaceOutboxRepository;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class PlaceEventPublisher {

    private final ObjectMapper objectMapper;
    private final PlaceOutboxRepository repository;

    public PlaceEventPublisher(
            ObjectMapper objectMapper,
            PlaceOutboxRepository repository
    ) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    public void publishCreated(Place place) {
        publish(PlaceEvent.created(toPayload(place), correlationId(), actorId()));
    }

    public void publishUpdated(Place place) {
        publish(PlaceEvent.updated(toPayload(place), correlationId(), actorId()));
    }

    private PlaceEventPayload toPayload(Place place) {
        return new PlaceEventPayload(
                place.getId(),
                place.getPartnerId(),
                place.getName(),
                place.getSlug(),
                place.getDescription(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory() != null ? place.getCategory().getSlug() : null,
                place.getRegion() != null ? place.getRegion().getCode() : null,
                place.getCity() != null ? place.getCity().getName() : null,
                place.getDistrict() != null ? place.getDistrict().getName() : null,
                place.getAddress(),
                place.getStatus() != null ? place.getStatus().name() : null,
                place.getUpdatedAt() != null ? place.getUpdatedAt() : place.getCreatedAt()
        );
    }

    private void publish(PlaceEvent event) {
        try {
            PlaceOutboxMessage message=new PlaceOutboxMessage();message.setId(event.eventId());
            message.setAggregateId(event.aggregateId());message.setEventType(event.eventType());
            message.setPayload(objectMapper.writeValueAsString(event));message.setOccurredAt(event.occurredAt());
            repository.save(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de serialiser l'evenement du lieu "+event.payload().placeId(),exception);
        }
    }

    private String correlationId() {
        String value = MDC.get("correlationId");
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }

    private String actorId() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        return authentication==null||!authentication.isAuthenticated()?"system":authentication.getName();
    }
}
