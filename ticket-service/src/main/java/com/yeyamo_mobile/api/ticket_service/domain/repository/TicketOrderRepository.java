package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrder;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketOrderRepository extends JpaRepository<TicketOrder, String> {
    
    Optional<TicketOrder> findByReference(String reference);
    
    List<TicketOrder> findByUserId(String userId);
    
    List<TicketOrder> findByEventId(String eventId);
    
    List<TicketOrder> findByUserIdAndEventId(String userId, String eventId);
    
    @Query("SELECT o FROM TicketOrder o WHERE o.userId = :userId AND o.status = :status")
    List<TicketOrder> findByUserIdAndStatus(String userId, TicketOrderStatus status);
    
    @Query("SELECT o FROM TicketOrder o WHERE o.status IN ('CREATED', 'AWAITING_PAYMENT') " +
           "AND o.expiresAt <= :now")
    List<TicketOrder> findExpiredOrders(Instant now);
    
    @Modifying
    @Query("UPDATE TicketOrder o SET o.status = 'EXPIRED' " +
           "WHERE o.status IN ('CREATED', 'AWAITING_PAYMENT') AND o.expiresAt <= :now")
    int expireOldOrders(Instant now);
    
    @Query("SELECT COUNT(o) FROM TicketOrder o WHERE o.userId = :userId " +
           "AND o.eventId = :eventId AND o.status IN ('PAID', 'ISSUED')")
    long countPaidOrdersByUserAndEvent(String userId, String eventId);
}
