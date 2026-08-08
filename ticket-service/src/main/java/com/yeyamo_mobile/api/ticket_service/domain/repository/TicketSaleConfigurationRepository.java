package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.SaleStatus;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketSaleConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketSaleConfigurationRepository extends JpaRepository<TicketSaleConfiguration, String> {
    
    Optional<TicketSaleConfiguration> findByEventId(String eventId);
    
    List<TicketSaleConfiguration> findByPartnerId(String partnerId);
    
    List<TicketSaleConfiguration> findByStatus(SaleStatus status);
    
    @Query("SELECT c FROM TicketSaleConfiguration c WHERE c.eventId = :eventId AND c.partnerId = :partnerId")
    Optional<TicketSaleConfiguration> findByEventIdAndPartnerId(String eventId, String partnerId);
    
    @Query("SELECT c FROM TicketSaleConfiguration c WHERE c.status = 'ACTIVE' " +
           "AND c.salesStartAt <= :now AND c.salesEndAt > :now")
    List<TicketSaleConfiguration> findActiveSalesAt(Instant now);
}
