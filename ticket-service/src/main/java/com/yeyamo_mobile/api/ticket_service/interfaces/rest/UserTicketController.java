package com.yeyamo_mobile.api.ticket_service.interfaces.rest;

import com.yeyamo_mobile.api.ticket_service.application.*;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "User Tickets", description = "User ticket operations")
@SecurityRequirement(name = "bearerAuth")
public class UserTicketController {
    
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final TicketIssuanceService ticketIssuanceService;
    
    public UserTicketController(
            InventoryService inventoryService,
            OrderService orderService,
            TicketIssuanceService ticketIssuanceService) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.ticketIssuanceService = ticketIssuanceService;
    }
    
    @PostMapping("/hold")
    @Operation(summary = "Create ticket hold (reserve tickets)")
    public ResponseEntity<HoldResponse> createHold(
            @Valid @RequestBody CreateHoldRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            Authentication auth) {
        
        String userId = auth.getName();
        
        TicketHoldEntity hold = inventoryService.createHold(
            new InventoryService.CreateHoldRequest(
                userId,
                request.eventId(),
                request.ticketTypeId(),
                request.quantity(),
                idempotencyKey
            )
        );
        
        return ResponseEntity.ok(new HoldResponse(
            hold.getId(),
            hold.getQuantity(),
            hold.getExpiresAt(),
            hold.getStatus()
        ));
    }
    
    @DeleteMapping("/hold/{holdId}")
    @Operation(summary = "Release hold manually")
    public ResponseEntity<Void> releaseHold(@PathVariable UUID holdId) {
        inventoryService.releaseHold(holdId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/orders")
    @Operation(summary = "Create order from hold")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth) {
        
        String userId = auth.getName();
        
        TicketOrderEntity order = orderService.createOrder(
            new OrderService.CreateOrderRequest(
                request.holdId(),
                userId,
                request.promotionCode(),
                request.operator(),
                request.phoneNumber(),
                countryClaim(auth)
            )
        );
        
        return ResponseEntity.ok(new OrderResponse(
            order.getId(),
            order.getReference(),
            order.getStatus(),
            order.getPaymentStatus(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getExpiresAt()
        ));
    }
    
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order details")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            Authentication auth) {
        
        TicketOrderEntity order = orderService.getUserOrders(auth.getName())
            .stream()
            .filter(o -> o.getId().equals(orderId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        return ResponseEntity.ok(new OrderResponse(
            order.getId(),
            order.getReference(),
            order.getStatus(),
            order.getPaymentStatus(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getExpiresAt()
        ));
    }
    
    @GetMapping("/my-orders")
    @Operation(summary = "Get user orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication auth) {
        List<TicketOrderEntity> orders = orderService.getUserOrders(auth.getName());
        
        List<OrderResponse> response = orders.stream()
            .map(o -> new OrderResponse(
                o.getId(),
                o.getReference(),
                o.getStatus(),
                o.getPaymentStatus(),
                o.getTotalAmount(),
                o.getCurrency(),
                o.getExpiresAt()
            ))
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my-tickets")
    @Operation(summary = "Get user tickets")
    public ResponseEntity<List<TicketSummary>> getMyTickets(
            @RequestParam(required = false) com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus status,
            Authentication auth) {
        List<TicketEntity> tickets = ticketIssuanceService.getUserTickets(auth.getName(), status);
        
        List<TicketSummary> response = tickets.stream()
            .map(t -> new TicketSummary(
                t.getId(),
                t.getSerialNumber(),
                t.getEventId(),
                t.getStatus(),
                t.getIssuedAt(),
                t.getUsedAt()
            ))
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get owned ticket details")
    public ResponseEntity<TicketDetailResponse> getTicket(@PathVariable UUID ticketId, Authentication auth) {
        TicketEntity ticket = ticketIssuanceService.getUserTicket(ticketId, auth.getName());
        return ResponseEntity.ok(new TicketDetailResponse(ticket.getId(), ticket.getOrderId(),
            ticket.getEventId(), ticket.getTicketTypeId(), ticket.getSerialNumber(), ticket.getStatus(),
            ticket.getIssuedAt(), ticket.getUsedAt(), ticket.getCancelledAt(), ticket.getRefundedAt(),
            ticket.getCreatedAt()));
    }
    
    @GetMapping("/{ticketId}/qr")
    @Operation(summary = "Get ticket QR code")
    public ResponseEntity<TicketQrResponse> getTicketQr(
            @PathVariable UUID ticketId,
            Authentication auth) {
        
        TicketIssuanceService.TicketWithQr ticketWithQr = ticketIssuanceService
            .getTicketWithQr(ticketId, auth.getName());
        
        TicketEntity ticket = ticketWithQr.ticket();
        
        return ResponseEntity.ok(new TicketQrResponse(
            ticket.getId(),
            ticket.getSerialNumber(),
            ticket.getEventId(),
            ticket.getStatus(),
            ticketWithQr.qrToken()
        ));
    }
    
    // DTOs
    public record CreateHoldRequest(
        @NotBlank String eventId,
        @NotNull UUID ticketTypeId,
        @Min(1) int quantity
    ) {}
    
    public record HoldResponse(
        UUID holdId,
        int quantity,
        java.time.Instant expiresAt,
        String status
    ) {}
    
    public record CreateOrderRequest(
        @NotNull UUID holdId,
        String promotionCode,
        @NotBlank @Pattern(regexp = "mtn|orange|moov|airtel|mpesa|wave|free|tmoney|afrimoney") String operator,
        @NotBlank @Pattern(regexp = "\\+[1-9]\\d{1,14}") String phoneNumber
    ) {}

    private String countryClaim(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("country");
        }
        return null;
    }
    
    public record OrderResponse(
        UUID orderId,
        String reference,
        com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus status,
        String paymentStatus,
        java.math.BigDecimal totalAmount,
        String currency,
        java.time.Instant expiresAt
    ) {}
    
    public record TicketSummary(
        UUID ticketId,
        String serialNumber,
        String eventId,
        com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus status,
        java.time.Instant issuedAt,
        java.time.Instant usedAt
    ) {}
    
    public record TicketQrResponse(
        UUID ticketId,
        String serialNumber,
        String eventId,
        com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus status,
        String qrToken
    ) {}

    public record TicketDetailResponse(UUID ticketId, UUID orderId, String eventId, UUID ticketTypeId,
        String serialNumber, com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus status,
        java.time.Instant issuedAt, java.time.Instant usedAt, java.time.Instant cancelledAt,
        java.time.Instant refundedAt, java.time.Instant createdAt) {}
}
