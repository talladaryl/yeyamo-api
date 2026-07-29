package com.yeyamo_mobile.api.event_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.models.Event;

public interface EventRepository extends JpaRepository<Event, UUID>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<Event> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(UUID id);

    List<Event> findByPlaceIdAndStatusOrderByStartAtAsc(UUID placeId, EventStatus status);

    List<Event> findByPlaceIdOrderByStartAtAsc(UUID placeId);

    List<Event> findByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus status, Instant after);
}
