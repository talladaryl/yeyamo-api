package com.yeyamo_mobile.api.booking_service.infrastructure.web;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.booking_service.application.*;
import com.yeyamo_mobile.api.booking_service.application.BookingDtos.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingApplicationService service;

    public BookingController(BookingApplicationService s) {
        service = s;
    }

    @GetMapping("/api/v1/activities")
    public Page<SlotView> listActivities(
            @RequestParam(required = false) UUID placeId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return service.listActivities(placeId, pageable);
    }

    @GetMapping("/api/v1/activities/{activityId}/availability")
    public List<SlotView> availability(@PathVariable String activityId) {
        return service.availability(activityId);
    }

    @PostMapping("/api/v1/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView create(
            Authentication a,
            @Valid @RequestBody CreateBooking body,
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String c
    ) {
        return service.create(a.getName(), body, requiredKey(key), c, countryClaim(a));
    }

    @GetMapping("/api/v1/bookings/me")
    public List<BookingView> mine(Authentication a) {
        return service.mine(a.getName());
    }

    @GetMapping("/api/v1/bookings/partner/me")
    public Page<BookingView> partnerMine(Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
        return service.partnerMine(authentication.getName(), pageable);
    }

    @GetMapping("/api/v1/bookings/{id}")
    public BookingView get(Authentication a, @PathVariable UUID id) {
        return service.get(a.getName(), id, admin(a));
    }

    @GetMapping("/api/v1/bookings/{id}/history")
    public List<HistoryView> history(Authentication a, @PathVariable UUID id) {
        return service.history(a.getName(), id, admin(a));
    }

    @PostMapping("/api/v1/bookings/{id}/cancel")
    public BookingView cancel(
            Authentication a,
            @PathVariable UUID id,
            @Valid @RequestBody CancelBooking body,
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader(value = "X-Correlation-Id", required = false) String c
    ) {
        return service.cancel(a.getName(), id, body.reason(), requiredKey(key), c, admin(a));
    }

    @PostMapping("/api/v1/booking-management/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotView slot(
            Authentication a,
            @Valid @RequestBody CreateSlot body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String c
    ) {
        return service.createSlot(body, a.getName(), c);
    }

    @PostMapping("/api/v1/booking-management/slots/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(
            Authentication a,
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String c
    ) {
        service.closeSlot(id, a.getName(), c, admin(a));
    }

    private String requiredKey(String key) {
        if (key == null || key.isBlank() || key.length() > 160) {
            throw new IllegalArgumentException("A valid Idempotency-Key is required");
        }
        return key;
    }

    private boolean admin(Authentication a) {
        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private String countryClaim(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("country");
        }
        return null;
    }
}
