package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.HoldStatus;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketHoldRepository extends JpaRepository<TicketHold, String> {
    
    Optional<TicketHold> findByIdempotencyKey(String idempotencyKey);
    
    List<TicketHold> findByUserIdAndEventId(String userId, String eventId);
    
    @Query("SELECT h FROM TicketHold h WHERE h.userId = :userId AND h.status = 'ACTIVE' " +
           "AND h.expiresAt > :now")
    List<TicketHold> findActiveHoldsByUser(String userId, Instant now);
    
    @Query("SELECT h FROM TicketHold h WHERE h.status = 'ACTIVE' AND h.expiresAt <= :now")
    List<TicketHold> findExpiredHolds(Instant now);
    
    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM TicketHold h " +
           "WHERE h.userId = :userId AND h.eventId = :eventId AND h.status = 'ACTIVE'")
    int countActiveHoldsByUserAndEvent(String userId, String eventId);
    
    @Modifying
    @Query("UPDATE TicketHold h SET h.status = 'EXPIRED' " +
           "WHERE h.status = 'ACTIVE' AND h.expiresAt <= :now")
    int expireOldHolds(Instant now);
}
