package com.yeyamo_mobile.api.ticket_service;

import com.yeyamo_mobile.api.ticket_service.application.dto.TicketScanRequest;
import com.yeyamo_mobile.api.ticket_service.application.dto.TicketScanResponse;
import com.yeyamo_mobile.api.ticket_service.application.service.TicketScanService;
import com.yeyamo_mobile.api.ticket_service.domain.model.*;
import com.yeyamo_mobile.api.ticket_service.domain.repository.*;
import com.yeyamo_mobile.api.ticket_service.infrastructure.security.QrTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test double scan prevention - concurrent scans should not both succeed
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TicketScanDoubleScanTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticket_test_db")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
    
    @Autowired
    private TicketScanService scanService;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketQrCredentialRepository qrCredentialRepository;
    
    @Autowired
    private EventStaffAssignmentRepository staffRepository;
    
    @Autowired
    private QrTokenService qrTokenService;
    
    private Ticket ticket;
    private String qrToken;
    private String scannerUserId;
    private String eventId;
    
    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID().toString();
        scannerUserId = "scanner-1";
        
        // Create staff assignment
        EventStaffAssignment staff = EventStaffAssignment.builder()
                .eventId(eventId)
                .partnerId("partner-1")
                .userId(scannerUserId)
                .role(StaffRole.ACCESS_CONTROLLER)
                .status(StaffStatus.ACTIVE)
                .validFrom(Instant.now().minus(1, ChronoUnit.HOURS))
                .validUntil(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        
        staffRepository.save(staff);
        
        // Create valid ticket
        ticket = Ticket.builder()
                .orderId(UUID.randomUUID().toString())
                .eventId(eventId)
                .ticketTypeId(UUID.randomUUID().toString())
                .ownerUserId("user-1")
                .serialNumber("TKT-" + System.currentTimeMillis())
                .status(TicketStatus.VALID)
                .issuedAt(Instant.now())
                .build();
        
        ticket = ticketRepository.save(ticket);
        
        // Generate QR credential
        QrTokenService.QrTokenData tokenData = qrTokenService.generateToken(
                ticket.getId(), 
                eventId, 
                365
        );
        
        qrToken = tokenData.token();
        
        TicketQrCredential credential = TicketQrCredential.builder()
                .ticketId(ticket.getId())
                .tokenId(tokenData.tokenId())
                .tokenHash(tokenData.tokenHash())
                .keyId(tokenData.keyId())
                .issuedAt(tokenData.issuedAt())
                .expiresAt(tokenData.expiresAt())
                .build();
        
        qrCredentialRepository.save(credential);
    }
    
    @Test
    void testConcurrentScansOnlyOneSucceeds() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyUsedCount = new AtomicInteger(0);
        
        // Try to scan same ticket concurrently from multiple threads
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    
                    TicketScanRequest request = TicketScanRequest.builder()
                            .qrToken(qrToken)
                            .eventId(eventId)
                            .gateId("gate-1")
                            .deviceId("device-" + Thread.currentThread().getId())
                            .build();
                    
                    TicketScanResponse response = scanService.scanTicket(scannerUserId, request);
                    
                    if (response.getResult() == ScanResult.VALID) {
                        successCount.incrementAndGet();
                    } else if (response.getResult() == ScanResult.ALREADY_USED) {
                        alreadyUsedCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    // Handle exceptions
                }
            });
        }
        
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
        
        // Exactly one scan should succeed
        assertThat(successCount.get()).isEqualTo(1);
        
        // Other scans should fail with ALREADY_USED
        assertThat(alreadyUsedCount.get()).isEqualTo(numberOfThreads - 1);
        
        // Verify ticket status
        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(updatedTicket.getUsedAt()).isNotNull();
    }
    
    @Test
    void testSequentialScanAfterUse() {
        // First scan - should succeed
        TicketScanRequest request = TicketScanRequest.builder()
                .qrToken(qrToken)
                .eventId(eventId)
                .gateId("gate-1")
                .build();
        
        TicketScanResponse response1 = scanService.scanTicket(scannerUserId, request);
        assertThat(response1.getResult()).isEqualTo(ScanResult.VALID);
        
        // Second scan - should fail with ALREADY_USED
        TicketScanResponse response2 = scanService.scanTicket(scannerUserId, request);
        assertThat(response2.getResult()).isEqualTo(ScanResult.ALREADY_USED);
        assertThat(response2.getUsedAt()).isNotNull();
    }
    
    @Test
    void testScanWithWrongEvent() {
        TicketScanRequest request = TicketScanRequest.builder()
                .qrToken(qrToken)
                .eventId("different-event-id")
                .gateId("gate-1")
                .build();
        
        TicketScanResponse response = scanService.scanTicket(scannerUserId, request);
        assertThat(response.getResult()).isEqualTo(ScanResult.WRONG_EVENT);
    }
}
