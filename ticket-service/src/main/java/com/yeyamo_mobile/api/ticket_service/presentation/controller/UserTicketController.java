package com.yeyamo_mobile.api.ticket_service.presentation.controller;

import com.yeyamo_mobile.api.ticket_service.application.dto.*;
import com.yeyamo_mobile.api.ticket_service.application.service.TicketService;
import com.yeyamo_mobile.api.ticket_service.domain.model.Ticket;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrder;
import com.yeyamo_mobile.api.ticket_service.domain.repository.TicketOrderRepository;
import com.yeyamo_mobile.api.ticket_service.domain.repository.TicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User APIs for ticket purchase and management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "User Tickets", description = "APIs for users to purchase and manage tickets")
@SecurityRequirement(name = "bearer-jwt")
public class UserTicketController {
    
    private final TicketService ticketService;
    private final TicketOrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    
    /**
     * Create a new ticket order
     */
    @PostMapping("/orders")
    @Operation(summary = "Create ticket order")
    public ResponseEntity<TicketOrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateTicketOrderRequest request) {
        
        String userId = extractUserId(authentication);
        String partnerId = "default"; // Extract from event context
        
        TicketOrderResponse response = ticketService.createOrder(userId, partnerId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get order details
     */
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order details")
    public ResponseEntity<TicketOrder> getOrder(
            Authentication authentication,
            @PathVariable String orderId) {
        
        String userId = extractUserId(authentication);
        
        TicketOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        if (!order.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(order);
    }
    
    /**
     * Get user's orders
     */
    @GetMapping("/orders")
    @Operation(summary = "Get my orders")
    public ResponseEntity<List<TicketOrder>> getMyOrders(Authentication authentication) {
        String userId = extractUserId(authentication);
        List<TicketOrder> orders = orderRepository.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Get user's tickets for an event
     */
    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get my tickets for event")
    public ResponseEntity<List<TicketResponse>> getMyTicketsForEvent(
            Authentication authentication,
            @PathVariable String eventId) {
        
        String userId = extractUserId(authentication);
        List<TicketResponse> tickets = ticketService.getUserTicketsForEvent(userId, eventId);
        
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get all user's tickets
     */
    @GetMapping
    @Operation(summary = "Get all my tickets")
    public ResponseEntity<List<Ticket>> getMyTickets(Authentication authentication) {
        String userId = extractUserId(authentication);
        List<Ticket> tickets = ticketRepository.findByOwnerUserId(userId);
        return ResponseEntity.ok(tickets);
    }
    
    /**
     * Get QR code for a ticket
     */
    @GetMapping("/{ticketId}/qr")
    @Operation(summary = "Get ticket QR code")
    public ResponseEntity<QrCodeResponse> getTicketQr(
            Authentication authentication,
            @PathVariable String ticketId) {
        
        String userId = extractUserId(authentication);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        
        if (!ticket.getOwnerUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        String qrToken = ticketService.generateQrCredential(ticketId);
        
        QrCodeResponse response = new QrCodeResponse(qrToken);
        return ResponseEntity.ok(response);
    }
    
    private String extractUserId(Authentication authentication) {
        return authentication.getName();
    }
    
    /**
     * QR Code response
     */
    public record QrCodeResponse(String qrToken) {}
}
