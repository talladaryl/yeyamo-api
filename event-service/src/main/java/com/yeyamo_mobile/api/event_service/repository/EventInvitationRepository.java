package com.yeyamo_mobile.api.event_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.event_service.models.EventInvitation;

public interface EventInvitationRepository extends JpaRepository<EventInvitation, UUID> {
    boolean existsByEventIdAndUserId(UUID eventId, String userId);
    Optional<EventInvitation> findByEventIdAndUserId(UUID eventId, String userId);
    List<EventInvitation> findByEventIdOrderByCreatedAtAsc(UUID eventId);
}
