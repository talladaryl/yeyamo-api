package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ticket_service.domain.model.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringTicketScanRepository extends JpaRepository<TicketScanEntity, UUID> {
    
    List<TicketScanEntity> findByTicketIdOrderByScannedAtAsc(UUID ticketId);
    
    List<TicketScanEntity> findByEventIdOrderByScannedAtDesc(String eventId);

    Optional<TicketScanEntity> findByScannerUserIdAndOfflineReference(String scannerUserId, String offlineReference);
    
    @Query("SELECT s FROM TicketScanEntity s WHERE s.ticketId = :ticketId AND s.result = 'VALID' ORDER BY s.scannedAt ASC")
    Optional<TicketScanEntity> findFirstSuccessfulScan(@Param("ticketId") UUID ticketId);
    
    @Query("SELECT s FROM TicketScanEntity s WHERE s.eventId = :eventId AND s.scannedAt BETWEEN :start AND :end ORDER BY s.scannedAt DESC")
    List<TicketScanEntity> findScansByEventAndTimeRange(
        @Param("eventId") String eventId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    @Query("SELECT COUNT(s) FROM TicketScanEntity s WHERE s.eventId = :eventId AND s.result = :result")
    long countByEventIdAndResult(@Param("eventId") String eventId, @Param("result") ScanResult result);
}
