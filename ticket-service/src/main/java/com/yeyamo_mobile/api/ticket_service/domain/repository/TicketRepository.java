package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.Ticket;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    
    Optional<Ticket> findBySerialNumber(String serialNumber);
    
    List<Ticket> findByOrderId(String orderId);
    
    List<Ticket> findByOwnerUserId(String ownerUserId);
    
    List<Ticket> findByEventId(String eventId);
    
    @Query("SELECT t FROM Ticket t WHERE t.ownerUserId = :userId AND t.eventId = :eventId")
    List<Ticket> findByUserAndEvent(String userId, String eventId);
    
    @Query("SELECT t FROM Ticket t WHERE t.ownerUserId = :userId AND t.status = :status")
    List<Ticket> findByUserAndStatus(String userId, TicketStatus status);
    
    /**
     * Find ticket by ID with pessimistic lock for scan operations
     * Prevents concurrent scan attempts on the same ticket
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithLock(String id);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.eventId = :eventId AND t.status = 'USED'")
    long countUsedTicketsByEvent(String eventId);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.eventId = :eventId AND t.status = 'VALID'")
    long countValidTicketsByEvent(String eventId);
}
