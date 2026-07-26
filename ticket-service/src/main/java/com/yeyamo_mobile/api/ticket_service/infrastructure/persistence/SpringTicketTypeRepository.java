package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

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
public interface SpringTicketTypeRepository extends JpaRepository<TicketTypeEntity, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketTypeEntity t WHERE t.id = :id")
    Optional<TicketTypeEntity> findByIdForUpdate(@Param("id") UUID id);
    
    List<TicketTypeEntity> findBySaleConfigurationId(UUID saleConfigurationId);
    
    Optional<TicketTypeEntity> findBySaleConfigurationIdAndCode(UUID saleConfigurationId, String code);
    
    @Query("SELECT t FROM TicketTypeEntity t WHERE t.saleConfigurationId = :configId AND t.status = 'ACTIVE'")
    List<TicketTypeEntity> findActiveTypesByConfiguration(@Param("configId") UUID configId);
}
