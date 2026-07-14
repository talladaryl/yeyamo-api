package com.yeyamo_mobile.api.event_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.models.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByPlaceIdAndStatusOrderByStartAtAsc(UUID placeId, EventStatus status);

    List<Event> findByPlaceIdOrderByStartAtAsc(UUID placeId);

    List<Event> findByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus status, Instant after);
}
