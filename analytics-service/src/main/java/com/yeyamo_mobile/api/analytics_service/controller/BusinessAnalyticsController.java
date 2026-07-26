package com.yeyamo_mobile.api.analytics_service.controller;

import com.yeyamo_mobile.api.analytics_service.business.BusinessAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class BusinessAnalyticsController {
    private final BusinessAnalyticsService analytics;

    public BusinessAnalyticsController(BusinessAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/partners/{partnerId}/campaigns/{campaignId}")
    @PreAuthorize("@analyticsAccess.canReadPartner(authentication,#partnerId)")
    @Operation(summary = "Campaign metrics with privacy-protected breakdowns")
    public Page<BusinessAnalyticsService.Metrics> campaign(
            @PathVariable UUID partnerId, @PathVariable String campaignId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "UTC") String timezone,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        validate(from, to, timezone);
        return analytics.campaign(partnerId.toString(), campaignId, from, to,
            PageRequest.of(page, size, Sort.by("statDate", "dimensionType")))
            .map(metric -> display(metric, timezone));
    }

    @GetMapping("/partners/{partnerId}/ticket-events/{eventId}")
    @PreAuthorize("@analyticsAccess.canReadPartner(authentication,#partnerId)")
    @Operation(summary = "Ticket sales, revenue and attendance metrics")
    public Page<BusinessAnalyticsService.Metrics> ticketing(
            @PathVariable UUID partnerId, @PathVariable String eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "UTC") String timezone,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        validate(from, to, timezone);
        return analytics.ticketing(partnerId.toString(), eventId, from, to,
            PageRequest.of(page, size, Sort.by("statDate", "dimensionType")))
            .map(metric -> display(metric, timezone));
    }

    @GetMapping("/partners/{partnerId}/ticket-events/{eventId}/peak-entry")
    @PreAuthorize("@analyticsAccess.canReadPartner(authentication,#partnerId)")
    @Operation(summary = "Peak ticket entry hour")
    public Optional<BusinessAnalyticsService.Metrics> peakEntry(
            @PathVariable UUID partnerId, @PathVariable String eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "UTC") String timezone) {
        validate(from, to, timezone);
        return analytics.peakEntry(partnerId.toString(), eventId, from, to)
            .map(metric -> display(metric, timezone));
    }

    @PostMapping("/admin/rebuild")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Rebuild all PostgreSQL projections from stored events")
    public Map<String, Long> rebuild() {
        return Map.of("eventsReplayed", analytics.rebuild());
    }

    @GetMapping("/admin/{scopeType}/{scopeId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Unsuppressed administrative business analytics")
    public Page<BusinessAnalyticsService.Metrics> admin(
            @PathVariable @Pattern(regexp = "CAMPAIGN|TICKET_EVENT") String scopeType,
            @PathVariable String scopeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "UTC") String timezone,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        validate(from, to, timezone);
        return analytics.admin(scopeType, scopeId, from, to,
            PageRequest.of(page, size, Sort.by("statDate", "dimensionType")))
            .map(metric -> display(metric, timezone));
    }

    private void validate(LocalDate from, LocalDate to, String timezone) {
        ZoneId.of(timezone);
        if (from.isAfter(to)) throw new IllegalArgumentException("from must precede to");
        if (Duration.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).toDays() > 366) {
            throw new IllegalArgumentException("range cannot exceed 366 days");
        }
    }

    private BusinessAnalyticsService.Metrics display(
            BusinessAnalyticsService.Metrics value, String timezone) {
        if (!"ENTRY_HOUR".equals(value.dimensionType()) || value.suppressed()) {
            return value;
        }
        int utcHour = Integer.parseInt(value.dimensionValue().substring(0, 2));
        String localHour = String.format("%02d:00",
            ZonedDateTime.of(value.date(), LocalTime.of(utcHour, 0), ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of(timezone)).getHour());
        return new BusinessAnalyticsService.Metrics(value.date(),
            value.dimensionType(), localHour, value.impressions(),
            value.qualifiedImpressions(), value.uniqueReach(), value.clicks(),
            value.conversions(), value.ticketsSold(), value.scans(),
            value.rejectedScans(), value.spend(), value.budget(),
            value.revenue(), value.commission(), value.refunds(),
            value.remainingBudget(), value.ctr(), value.conversionRate(),
            value.cpm(), value.cpc(), value.cpa(), value.attendanceRate(),
            value.suppressed());
    }
}
