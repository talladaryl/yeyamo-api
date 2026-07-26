package com.yeyamo_mobile.api.ticket_service.application;

import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    private final SpringTicketHoldRepository holdRepository;
    private final SpringTicketTypeRepository ticketTypeRepository;
    private final SpringTicketSaleConfigurationRepository configurationRepository;
    private final SpringTicketRepository ticketRepository;
    private final int holdTtlMinutes;
    
    public InventoryService(
            SpringTicketHoldRepository holdRepository,
            SpringTicketTypeRepository ticketTypeRepository,
            SpringTicketSaleConfigurationRepository configurationRepository,
            SpringTicketRepository ticketRepository,
            @Value("${yeyamo.tickets.hold-ttl-minutes:10}") int holdTtlMinutes) {
        this.holdRepository = holdRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.configurationRepository = configurationRepository;
        this.ticketRepository = ticketRepository;
        this.holdTtlMinutes = holdTtlMinutes;
    }
    
    /**
     * Create a hold (reservation) for tickets
     * Idempotent operation
     */
    @Transactional
    public TicketHoldEntity createHold(CreateHoldRequest request) {
        // 1. Idempotency check
        Optional<TicketHoldEntity> existing = holdRepository
            .findByUserIdAndIdempotencyKey(request.userId(), request.idempotencyKey());
        
        if (existing.isPresent()) {
            logger.info("Hold already exists for idempotency key: {}", request.idempotencyKey());
            return existing.get();
        }
        
        // 2. Load ticket type with pessimistic lock
        TicketTypeEntity ticketType = ticketTypeRepository
            .findByIdForUpdate(request.ticketTypeId())
            .orElseThrow(() -> new IllegalArgumentException("Ticket type not found: " + request.ticketTypeId()));

        TicketSaleConfigurationEntity configuration = configurationRepository
            .findById(ticketType.getSaleConfigurationId())
            .orElseThrow(() -> new IllegalStateException("Sale configuration not found"));
        Instant now = Instant.now();
        validateSaleWindow(request, ticketType, configuration, now);

        long buyerQuantity = holdRepository.sumReservedByUserAndEvent(
            request.userId(), request.eventId(), now
        ) + ticketRepository.countOwnedTicketsForEvent(request.userId(), request.eventId());
        if (buyerQuantity + request.quantity() > configuration.getMaxTicketsPerBuyer()) {
            throw new IllegalArgumentException("Maximum tickets per buyer exceeded");
        }
        
        // 3. Validate availability
        if (!ticketType.canReserve(request.quantity())) {
            int available = ticketType.getAvailableQuantity();
            throw new InsufficientInventoryException(
                String.format("Cannot reserve %d tickets. Only %d available", 
                    request.quantity(), available)
            );
        }
        
        // 4. Reserve inventory (optimistic locking)
        ticketType.reserve(request.quantity());
        ticketTypeRepository.save(ticketType);
        
        // 5. Create hold
        TicketHoldEntity hold = new TicketHoldEntity();
        hold.setUserId(request.userId());
        hold.setEventId(request.eventId());
        hold.setTicketTypeId(ticketType.getId());
        hold.setQuantity(request.quantity());
        hold.setExpiresAt(Instant.now().plusSeconds(holdTtlMinutes * 60L));
        hold.setStatus("ACTIVE");
        hold.setIdempotencyKey(request.idempotencyKey());
        
        hold = holdRepository.save(hold);
        
        logger.info("Created hold: {} for user: {} ({} tickets)", 
            hold.getId(), request.userId(), request.quantity());
        
        return hold;
    }

    private void validateSaleWindow(
            CreateHoldRequest request,
            TicketTypeEntity ticketType,
            TicketSaleConfigurationEntity configuration,
            Instant now) {
        if (!configuration.getEventId().equals(request.eventId())) {
            throw new IllegalArgumentException("Ticket type does not belong to event");
        }
        if (!"ACTIVE".equals(configuration.getStatus()) || !"ACTIVE".equals(ticketType.getStatus())) {
            throw new IllegalStateException("Ticket sales are not active");
        }
        Instant startsAt = ticketType.getSalesStartAt() != null
            ? ticketType.getSalesStartAt() : configuration.getSalesStartAt();
        Instant endsAt = ticketType.getSalesEndAt() != null
            ? ticketType.getSalesEndAt() : configuration.getSalesEndAt();
        if (now.isBefore(startsAt) || !now.isBefore(endsAt)) {
            throw new IllegalStateException("Ticket sales are outside the allowed period");
        }
    }
    
    /**
     * Release expired holds automatically
     * Scheduled job runs every 5 minutes
     */
    @Scheduled(cron = "${yeyamo.tickets.scheduler.release-holds-cron:0 */5 * * * *}")
    @Transactional
    public void releaseExpiredHolds() {
        List<TicketHoldEntity> expiredHolds = holdRepository.findExpiredHolds(Instant.now());
        
        if (expiredHolds.isEmpty()) {
            return;
        }
        
        logger.info("Found {} expired holds to release", expiredHolds.size());
        
        for (TicketHoldEntity hold : expiredHolds) {
            try {
                // Load ticket type with pessimistic lock
                TicketTypeEntity ticketType = ticketTypeRepository
                    .findByIdForUpdate(hold.getTicketTypeId())
                    .orElseThrow();
                
                // Release reservation
                ticketType.releaseReservation(hold.getQuantity());
                ticketTypeRepository.save(ticketType);
                
                // Mark hold as expired
                hold.setStatus("EXPIRED");
                holdRepository.save(hold);
                
                logger.info("Released expired hold: {} ({} tickets)", 
                    hold.getId(), hold.getQuantity());
                
            } catch (Exception e) {
                logger.error("Failed to release hold: " + hold.getId(), e);
            }
        }
        
        logger.info("Released {} expired holds", expiredHolds.size());
    }
    
    /**
     * Release a hold manually (e.g., user cancels cart)
     */
    @Transactional
    public void releaseHold(UUID holdId) {
        TicketHoldEntity hold = holdRepository.findById(holdId)
            .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));
        
        if (!"ACTIVE".equals(hold.getStatus())) {
            logger.warn("Hold is not active: {}", holdId);
            return;
        }
        
        // Load ticket type with pessimistic lock
        TicketTypeEntity ticketType = ticketTypeRepository
            .findByIdForUpdate(hold.getTicketTypeId())
            .orElseThrow();
        
        // Release reservation
        ticketType.releaseReservation(hold.getQuantity());
        ticketTypeRepository.save(ticketType);
        
        // Mark hold as released
        hold.setStatus("RELEASED");
        holdRepository.save(hold);
        
        logger.info("Manually released hold: {} ({} tickets)", holdId, hold.getQuantity());
    }
    
    /**
     * Confirm sale (convert reservation to sold)
     * Called after payment confirmed
     */
    @Transactional
    public void confirmSale(UUID ticketTypeId, int quantity) {
        TicketTypeEntity ticketType = ticketTypeRepository
            .findByIdForUpdate(ticketTypeId)
            .orElseThrow();
        
        ticketType.confirmSale(quantity);
        ticketTypeRepository.save(ticketType);
        
        logger.info("Confirmed sale: {} tickets for type: {}", quantity, ticketTypeId);
    }
    
    /**
     * Compensate failed payment (release reservation)
     */
    @Transactional
    public void compensateFailedPayment(UUID holdId) {
        TicketHoldEntity hold = holdRepository.findByOrderId(holdId)
            .orElseThrow(() -> new IllegalArgumentException("Hold not found for order: " + holdId));
        
        if (!"CONSUMED".equals(hold.getStatus())) {
            return; // Already released or expired
        }
        
        TicketTypeEntity ticketType = ticketTypeRepository
            .findByIdForUpdate(hold.getTicketTypeId())
            .orElseThrow();
        
        ticketType.releaseReservation(hold.getQuantity());
        ticketTypeRepository.save(ticketType);
        
        hold.setStatus("FAILED");
        holdRepository.save(hold);
        
        logger.warn("Compensated failed payment, released hold: {}", hold.getId());
    }
    
    // DTOs
    public record CreateHoldRequest(
        String userId,
        String eventId,
        UUID ticketTypeId,
        int quantity,
        String idempotencyKey
    ) {
        public CreateHoldRequest {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
        }
    }
    
    // Exceptions
    public static class InsufficientInventoryException extends RuntimeException {
        public InsufficientInventoryException(String message) {
            super(message);
        }
    }
}
