package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringTicketHoldRepository extends JpaRepository<TicketHoldEntity, UUID> {
    
    Optional<TicketHoldEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
    
    Optional<TicketHoldEntity> findByOrderId(UUID orderId);
    
    @Query("SELECT h FROM TicketHoldEntity h WHERE h.status = 'ACTIVE' AND h.expiresAt < :now")
    List<TicketHoldEntity> findExpiredHolds(@Param("now") Instant now);
    
    @Query("SELECT h FROM TicketHoldEntity h WHERE h.userId = :userId AND h.status = 'ACTIVE' ORDER BY h.createdAt DESC")
    List<TicketHoldEntity> findActiveHoldsByUser(@Param("userId") String userId);
    
    @Query("SELECT COUNT(h) FROM TicketHoldEntity h WHERE h.userId = :userId AND h.eventId = :eventId AND h.status = 'ACTIVE'")
    long countActiveHoldsByUserAndEvent(@Param("userId") String userId, @Param("eventId") String eventId);

    @Query("""
        SELECT COALESCE(SUM(h.quantity), 0) FROM TicketHoldEntity h
        WHERE h.userId = :userId AND h.eventId = :eventId
          AND h.status IN ('ACTIVE', 'CONSUMED') AND h.expiresAt > :now
        """)
    long sumReservedByUserAndEvent(
        @Param("userId") String userId,
        @Param("eventId") String eventId,
        @Param("now") Instant now
    );
}
