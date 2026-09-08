package com.yeyamo_mobile.api.ticket_service.application.service;

import com.yeyamo_mobile.api.ticket_service.domain.model.*;
import com.yeyamo_mobile.api.ticket_service.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages ticket inventory with strict concurrency controls.
 * Uses distributed locks and optimistic locking to prevent overselling.
 */
@Slf4j
@Service
@Profile("legacy-ticket-api")
@RequiredArgsConstructor
public class TicketInventoryService {
    
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketHoldRepository holdRepository;
    private final TicketSaleConfigurationRepository saleConfigurationRepository;
    private final RedissonClient redissonClient;
    
    private static final int LOCK_TIMEOUT_SECONDS = 5;
    private static final int HOLD_TTL_MINUTES = 10;
    
    /**
     * Create a temporary hold on tickets with distributed lock
     * 
     * @param userId User creating the hold
     * @param ticketTypeId Ticket type to hold
     * @param quantity Number of tickets to hold
     * @param idempotencyKey Unique key to prevent duplicate holds
     * @return Created hold
     */
    @Transactional
    public TicketHold createHold(String userId, String ticketTypeId, int quantity, 
                                 String eventId, String idempotencyKey) {
        // Check for existing hold with idempotency key
        Optional<TicketHold> existing = holdRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Returning existing hold for idempotency key: {}", idempotencyKey);
            return existing.get();
        }
        
        String lockKey = "ticket:inventory:" + ticketTypeId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Acquire distributed lock
            boolean lockAcquired = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new IllegalStateException("Could not acquire inventory lock");
            }
            
            try {
                // Load ticket type with pessimistic lock
                TicketType ticketType = ticketTypeRepository.findByIdWithLock(ticketTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("Ticket type not found"));
                
                // Validate ticket type is active and has inventory
                if (ticketType.getStatus() != SaleStatus.ACTIVE) {
                    throw new IllegalStateException("Ticket type is not available for sale");
                }
                
                if (!ticketType.hasAvailableTickets(quantity)) {
                    throw new IllegalStateException("Insufficient ticket inventory");
                }
                
                // Reserve inventory
                ticketType.reserveTickets(quantity);
                ticketTypeRepository.save(ticketType);
                
                // Create hold
                Instant expiresAt = Instant.now().plus(HOLD_TTL_MINUTES, ChronoUnit.MINUTES);
                TicketHold hold = TicketHold.builder()
                        .userId(userId)
                        .eventId(eventId)
                        .ticketTypeId(ticketTypeId)
                        .quantity(quantity)
                        .expiresAt(expiresAt)
                        .status(HoldStatus.ACTIVE)
                        .idempotencyKey(idempotencyKey)
                        .build();
                
                hold = holdRepository.save(hold);
                
                log.info("Created hold: {} for user: {}, quantity: {}, expires: {}", 
                         hold.getId(), userId, quantity, expiresAt);
                
                return hold;
                
            } finally {
                lock.unlock();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        }
    }
    
    /**
     * Release a hold and return inventory
     */
    @Transactional
    public void releaseHold(String holdId) {
        TicketHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found"));
        
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            log.warn("Cannot release hold {} - status is {}", holdId, hold.getStatus());
            return;
        }
        
        String lockKey = "ticket:inventory:" + hold.getTicketTypeId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            lock.lock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            try {
                TicketType ticketType = ticketTypeRepository.findByIdWithLock(hold.getTicketTypeId())
                        .orElseThrow(() -> new IllegalStateException("Ticket type not found"));
                
                ticketType.releaseReservedTickets(hold.getQuantity());
                ticketTypeRepository.save(ticketType);
                
                hold.setStatus(HoldStatus.RELEASED);
                holdRepository.save(hold);
                
                log.info("Released hold: {}, quantity: {}", holdId, hold.getQuantity());
                
            } finally {
                lock.unlock();
            }
            
        } catch (Exception e) {
            log.error("Error releasing hold: {}", holdId, e);
            throw new RuntimeException("Failed to release hold", e);
        }
    }
    
    /**
     * Confirm hold and convert to sold inventory
     */
    @Transactional
    public void confirmHold(String holdId) {
        TicketHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found"));
        
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active");
        }
        
        String lockKey = "ticket:inventory:" + hold.getTicketTypeId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            lock.lock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            try {
                TicketType ticketType = ticketTypeRepository.findByIdWithLock(hold.getTicketTypeId())
                        .orElseThrow(() -> new IllegalStateException("Ticket type not found"));
                
                ticketType.confirmSale(hold.getQuantity());
                ticketTypeRepository.save(ticketType);
                
                hold.setStatus(HoldStatus.CONFIRMED);
                holdRepository.save(hold);
                
                log.info("Confirmed hold: {}, quantity: {}", holdId, hold.getQuantity());
                
            } finally {
                lock.unlock();
            }
            
        } catch (Exception e) {
            log.error("Error confirming hold: {}", holdId, e);
            throw new RuntimeException("Failed to confirm hold", e);
        }
    }
    
    /**
     * Expire old holds and release inventory
     */
    @Transactional
    public int expireOldHolds() {
        Instant now = Instant.now();
        var expiredHolds = holdRepository.findExpiredHolds(now);
        
        int count = 0;
        for (TicketHold hold : expiredHolds) {
            try {
                releaseHold(hold.getId());
                count++;
            } catch (Exception e) {
                log.error("Failed to expire hold: {}", hold.getId(), e);
            }
        }
        
        log.info("Expired {} holds", count);
        return count;
    }
    
    /**
     * Check if user can purchase more tickets for an event
     */
    public boolean canUserPurchaseMore(String userId, String eventId, int requestedQuantity) {
        // Get sale configuration
        TicketSaleConfiguration config = saleConfigurationRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Sale configuration not found"));
        
        // Count active holds
        int activeHolds = holdRepository.countActiveHoldsByUserAndEvent(userId, eventId);
        
        // Count paid orders
        long paidOrders = 0; // Would need to query orders
        
        int total = activeHolds + (int) paidOrders + requestedQuantity;
        
        return total <= config.getMaxTicketsPerBuyer();
    }
}
