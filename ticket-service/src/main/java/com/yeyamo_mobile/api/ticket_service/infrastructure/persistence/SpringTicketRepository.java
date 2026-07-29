package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringTicketRepository extends JpaRepository<TicketEntity, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketEntity t WHERE t.id = :id")
    Optional<TicketEntity> findByIdForUpdate(@Param("id") UUID id);
    
    Optional<TicketEntity> findBySerialNumber(String serialNumber);
    
    List<TicketEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
    
    List<TicketEntity> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);
    
    @Query("SELECT t FROM TicketEntity t WHERE t.ownerUserId = :userId AND t.status = :status ORDER BY t.createdAt DESC")
    List<TicketEntity> findByOwnerUserIdAndStatus(@Param("userId") String userId, @Param("status") TicketStatus status);
    
    @Query("SELECT t FROM TicketEntity t WHERE t.eventId = :eventId AND t.status = 'VALID' ORDER BY t.createdAt DESC")
    List<TicketEntity> findValidTicketsByEvent(@Param("eventId") String eventId);
    
    @Query("SELECT COUNT(t) FROM TicketEntity t WHERE t.eventId = :eventId AND t.status = 'USED'")
    long countUsedTicketsByEvent(@Param("eventId") String eventId);
    List<TicketEntity> findByEventIdOrderByCreatedAtDesc(String eventId);
    long countByEventIdAndStatus(String eventId,TicketStatus status);

    @Query("""
        SELECT COUNT(t) FROM TicketEntity t
        WHERE t.ownerUserId = :userId AND t.eventId = :eventId
          AND t.status NOT IN ('CANCELLED', 'REFUNDED', 'EXPIRED', 'REVOKED')
        """)
    long countOwnedTicketsForEvent(@Param("userId") String userId, @Param("eventId") String eventId);
}
