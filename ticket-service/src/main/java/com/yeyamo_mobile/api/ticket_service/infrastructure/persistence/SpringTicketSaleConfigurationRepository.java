package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringTicketSaleConfigurationRepository extends JpaRepository<TicketSaleConfigurationEntity, UUID> {
    
    Optional<TicketSaleConfigurationEntity> findByEventId(String eventId);
    
    List<TicketSaleConfigurationEntity> findByPartnerId(String partnerId);
    
    @Query("SELECT c FROM TicketSaleConfigurationEntity c WHERE c.status = 'ACTIVE' ORDER BY c.createdAt DESC")
    List<TicketSaleConfigurationEntity> findActiveConfigurations();
}
