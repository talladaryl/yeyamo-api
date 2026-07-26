package com.yeyamo_mobile.api.ticket_service.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringOutboxRepository extends JpaRepository<TicketOutboxEntity, UUID> {
    
    @Query("SELECT o FROM TicketOutboxEntity o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<TicketOutboxEntity> findPendingEvents();
    
    @Query("SELECT o FROM TicketOutboxEntity o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC LIMIT 100")
    List<TicketOutboxEntity> findPendingEventsBatch();
}
