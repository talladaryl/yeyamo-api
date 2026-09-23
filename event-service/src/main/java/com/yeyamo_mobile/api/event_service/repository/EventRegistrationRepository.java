package com.yeyamo_mobile.api.event_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import com.yeyamo_mobile.api.event_service.enums.RegistrationStatus;
import com.yeyamo_mobile.api.event_service.models.EventRegistration;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    Optional<EventRegistration> findByEventIdAndUserId(UUID eventId, String userId);

    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, String userId, RegistrationStatus status);

    List<EventRegistration> findByUserIdAndStatusOrderByRegisteredAtDesc(
            String userId, RegistrationStatus status, Pageable pageable);

    List<EventRegistration> findByEventIdAndStatusOrderByRegisteredAtAsc(
            UUID eventId, RegistrationStatus status, Pageable pageable);

    @Query("select r.userId from EventRegistration r where r.event.id = :eventId and r.status = :status")
    List<String> findUserIdsByEventIdAndStatus(UUID eventId, RegistrationStatus status);
}
