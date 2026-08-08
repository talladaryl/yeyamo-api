package com.yeyamo_mobile.api.ticket_service;

import com.yeyamo_mobile.api.ticket_service.application.service.TicketInventoryService;
import com.yeyamo_mobile.api.ticket_service.domain.model.*;
import com.yeyamo_mobile.api.ticket_service.domain.repository.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test concurrent inventory operations to ensure no overselling
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TicketInventoryConcurrencyTest {
    
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
    private TicketInventoryService inventoryService;
    
    @Autowired
    private TicketSaleConfigurationRepository saleConfigRepository;
    
    @Autowired
    private TicketTypeRepository ticketTypeRepository;
    
    @Autowired
    private TicketHoldRepository holdRepository;
    
    private TicketType ticketType;
    private String eventId;
    
    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID().toString();
        
        // Create sale configuration
        TicketSaleConfiguration config = TicketSaleConfiguration.builder()
                .eventId(eventId)
                .partnerId("partner-1")
                .salesStartAt(Instant.now())
                .salesEndAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .status(SaleStatus.ACTIVE)
                .maxTicketsPerBuyer(10)
                .currency("XOF")
                .build();
        
        config = saleConfigRepository.save(config);
        
        // Create ticket type with limited inventory
        ticketType = TicketType.builder()
                .saleConfigurationId(config.getId())
                .code("VIP")
                .name("VIP Ticket")
                .price(BigDecimal.valueOf(50000))
                .quantityTotal(10)  // Only 10 tickets available
                .quantityReserved(0)
                .quantitySold(0)
                .status(SaleStatus.ACTIVE)
                .build();
        
        ticketType = ticketTypeRepository.save(ticketType);
    }
    
    @Test
    void testConcurrentHoldsDoNotOversell() throws InterruptedException {
        int numberOfThreads = 20;
        int ticketsPerThread = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        List<Future<Boolean>> futures = new ArrayList<>();
        
        // Try to create holds concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            Future<Boolean> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    
                    String userId = "user-" + threadNum;
                    String idempotencyKey = UUID.randomUUID().toString();
                    
                    inventoryService.createHold(
                            userId, 
                            ticketType.getId(), 
                            ticketsPerThread,
                            eventId,
                            idempotencyKey
                    );
                    
                    return true;
                    
                } catch (Exception e) {
                    // Expected for some threads when inventory runs out
                    return false;
                }
            });
            
            futures.add(future);
        }
        
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
        
        // Count successful holds
        long successfulHolds = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .filter(success -> success)
                .count();
        
        // Verify inventory consistency
        TicketType updatedTicketType = ticketTypeRepository.findById(ticketType.getId()).orElseThrow();
        
        // Only 10 tickets should be reserved (not more)
        assertThat(updatedTicketType.getQuantityReserved()).isEqualTo(10);
        assertThat(updatedTicketType.getQuantitySold()).isEqualTo(0);
        assertThat(successfulHolds).isEqualTo(10);
        
        // Verify available inventory is 0
        assertThat(updatedTicketType.getAvailableQuantity()).isEqualTo(0);
    }
    
    @Test
    void testIdempotentHoldCreation() {
        String userId = "user-1";
        String idempotencyKey = UUID.randomUUID().toString();
        
        // Create hold first time
        TicketHold hold1 = inventoryService.createHold(
                userId, 
                ticketType.getId(), 
                2,
                eventId,
                idempotencyKey
        );
        
        // Try to create same hold again with same idempotency key
        TicketHold hold2 = inventoryService.createHold(
                userId, 
                ticketType.getId(), 
                2,
                eventId,
                idempotencyKey
        );
        
        // Should return same hold
        assertThat(hold1.getId()).isEqualTo(hold2.getId());
        
        // Inventory should only be reserved once
        TicketType updatedTicketType = ticketTypeRepository.findById(ticketType.getId()).orElseThrow();
        assertThat(updatedTicketType.getQuantityReserved()).isEqualTo(2);
    }
}
