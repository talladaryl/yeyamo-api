package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, String> {
    
    List<TicketType> findBySaleConfigurationId(String saleConfigurationId);
    
    Optional<TicketType> findBySaleConfigurationIdAndCode(String saleConfigurationId, String code);
    
    /**
     * Find ticket type with pessimistic write lock for inventory operations
     * Prevents concurrent modifications to inventory
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findByIdWithLock(String id);
    
    /**
     * Find all ticket types for a sale configuration with locks
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.saleConfigurationId = :saleConfigurationId")
    List<TicketType> findBySaleConfigurationIdWithLock(String saleConfigurationId);
    
    @Query("SELECT t FROM TicketType t WHERE t.saleConfigurationId = :configId AND t.status = 'ACTIVE'")
    List<TicketType> findActiveTicketTypes(String configId);
}
