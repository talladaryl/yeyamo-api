package com.yeyamo_mobile.api.ticket_service.domain.repository;

import com.yeyamo_mobile.api.ticket_service.domain.model.ScanResult;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketScanRepository extends JpaRepository<TicketScan, String> {
    
    List<TicketScan> findByTicketId(String ticketId);
    
    List<TicketScan> findByEventId(String eventId);
    
    @Query("SELECT s FROM TicketScan s WHERE s.ticketId = :ticketId ORDER BY s.scannedAt DESC")
    List<TicketScan> findByTicketIdOrderByScannedAtDesc(String ticketId);
    
    @Query("SELECT s FROM TicketScan s WHERE s.ticketId = :ticketId AND s.result = 'VALID'")
    Optional<TicketScan> findSuccessfulScanByTicketId(String ticketId);
    
    @Query("SELECT COUNT(s) FROM TicketScan s WHERE s.eventId = :eventId AND s.result = 'VALID'")
    long countSuccessfulScansByEvent(String eventId);
    
    @Query("SELECT COUNT(s) FROM TicketScan s WHERE s.eventId = :eventId " +
           "AND s.scannedAt BETWEEN :startTime AND :endTime")
    long countScansByEventAndTimeRange(String eventId, Instant startTime, Instant endTime);
    
    @Query("SELECT s.result, COUNT(s) FROM TicketScan s WHERE s.eventId = :eventId GROUP BY s.result")
    List<Object[]> countScanResultsByEvent(String eventId);
    
    @Query("SELECT s FROM TicketScan s WHERE s.scannerUserId = :scannerId ORDER BY s.scannedAt DESC")
    List<TicketScan> findByScannerUserId(String scannerId);
}
