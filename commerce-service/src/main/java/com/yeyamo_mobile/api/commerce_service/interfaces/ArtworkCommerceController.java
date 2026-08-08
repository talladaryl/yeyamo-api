package com.yeyamo_mobile.api.commerce_service.interfaces;

import com.yeyamo_mobile.api.commerce_service.application.ArtworkCommerceService;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkOffer;
import com.yeyamo_mobile.api.commerce_service.persistence.ArtworkOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ArtworkCommerceController {
    private final ArtworkCommerceService service;

    public ArtworkCommerceController(ArtworkCommerceService service) {
        this.service = service;
    }

    public record OfferRequest(@NotNull UUID artworkId, @NotBlank String artisanPartnerId,
            @NotNull ArtworkOffer.SaleType saleType, @Positive BigDecimal amount,
            @Pattern(regexp = "^[A-Z]{3}$") String currencyCode, @PositiveOrZero int availableQuantity,
            @Pattern(regexp = "^[A-Z]{2}$") String countryCode, boolean internationalShipping,
            boolean customOrderAllowed, @NotNull ArtworkOffer.Status status) {
    }
    public record OfferStatus(@NotNull ArtworkOffer.Status status) { }
    public record OrderRequest(@NotNull UUID offerId, @Min(1) int quantity, @NotNull ArtworkOrder.DeliveryType deliveryType) { }
    public record CancelRequest(@NotBlank String reason) { }
    public record StatusRequest(@NotNull ArtworkOrder.Status status, @NotBlank String reason) { }

    @GetMapping("/artwork-offers/{artworkId}")
    public ArtworkOffer offer(@PathVariable UUID artworkId) { return service.offer(artworkId).orElseThrow(); }

    @PostMapping("/artwork-offers")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtworkOffer createOffer(@Valid @RequestBody OfferRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.createOffer(command(request), partner(jwt), admin(jwt));
    }

    @PutMapping("/artwork-offers/{id}")
    public ArtworkOffer updateOffer(@PathVariable UUID id, @Valid @RequestBody OfferRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.updateOffer(id, command(request), partner(jwt), admin(jwt));
    }

    @PatchMapping("/artwork-offers/{id}/status")
    public ArtworkOffer offerStatus(@PathVariable UUID id, @Valid @RequestBody OfferStatus request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.status(id, request.status(), partner(jwt), admin(jwt));
    }

    @PostMapping("/artwork-orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtworkOrder order(@Valid @RequestBody OrderRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.createOrder(new ArtworkCommerceService.OrderCommand(request.offerId(), request.quantity(), request.deliveryType()),
                jwt.getSubject(), idempotencyKey, correlationId);
    }

    @GetMapping("/artwork-orders/me")
    public List<ArtworkOrder> mine(@AuthenticationPrincipal Jwt jwt) { return service.mine(jwt.getSubject()); }

    @GetMapping("/artwork-orders/{id}")
    public ArtworkOrder detail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.detail(id, admin(jwt) ? partner(jwt) : jwt.getSubject(), admin(jwt));
    }

    @PostMapping("/artwork-orders/{id}/cancel")
    public ArtworkOrder cancel(@PathVariable UUID id, @Valid @RequestBody CancelRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.cancel(id, jwt.getSubject(), request.reason(), correlationId);
    }

    @GetMapping("/artisan/orders")
    public List<ArtworkOrder> artisan(@AuthenticationPrincipal Jwt jwt) { return service.artisan(partner(jwt)); }

    @GetMapping("/artisan/orders/{id}")
    public ArtworkOrder artisanDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.detail(id, partner(jwt), admin(jwt));
    }

    @PatchMapping("/artisan/orders/{id}/status")
    public ArtworkOrder artisanStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        return service.artisanStatus(id, request.status(), partner(jwt), admin(jwt), request.reason(), correlationId);
    }

    private ArtworkCommerceService.OfferCommand command(OfferRequest request) {
        return new ArtworkCommerceService.OfferCommand(request.artworkId(), request.artisanPartnerId(), request.saleType(),
                request.amount(), request.currencyCode(), request.availableQuantity(), request.countryCode(),
                request.internationalShipping(), request.customOrderAllowed(), request.status());
    }

    private boolean admin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"));
    }

    private String partner(Jwt jwt) {
        Object partnerId = jwt.getClaims().get("partnerId");
        if (partnerId != null) return partnerId.toString();
        if (admin(jwt)) return jwt.getSubject();
        throw new SecurityException("Partner identity missing");
    }
}
