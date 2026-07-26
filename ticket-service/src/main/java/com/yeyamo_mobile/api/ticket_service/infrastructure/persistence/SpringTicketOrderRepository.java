package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringTicketOrderRepository extends JpaRepository<TicketOrderEntity, UUID> {
    
    Optional<TicketOrderEntity> findByReference(String reference);
    
    Optional<TicketOrderEntity> findByPaymentReference(String paymentReference);
    
    List<TicketOrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<TicketOrderEntity> findByEventIdOrderByCreatedAtDesc(String eventId);
    
    @Query("SELECT o FROM TicketOrderEntity o WHERE o.userId = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    List<TicketOrderEntity> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") TicketOrderStatus status);
    
    @Query("SELECT o FROM TicketOrderEntity o WHERE o.eventId = :eventId AND o.status = 'ISSUED' ORDER BY o.createdAt DESC")
    List<TicketOrderEntity> findIssuedOrdersByEvent(@Param("eventId") String eventId);
}
