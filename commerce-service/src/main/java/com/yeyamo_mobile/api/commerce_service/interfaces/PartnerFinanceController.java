package com.yeyamo_mobile.api.commerce_service.interfaces;

import com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.LedgerType;
import com.yeyamo_mobile.api.commerce_service.persistence.LedgerEntry;
import com.yeyamo_mobile.api.commerce_service.persistence.LedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1/commerce/partners/{partnerId}/finance")
public class PartnerFinanceController {
    private final LedgerRepository ledger;
    private final RestClient partners;

    public PartnerFinanceController(LedgerRepository ledger,
            @Value("${yeyamo.services.partner-url:http://partner-service:8080}") String partnerUrl) {
        this.ledger = ledger;
        this.partners = RestClient.create(partnerUrl);
    }

    public record Summary(BigDecimal balance, BigDecimal grossRevenue, BigDecimal commissions,
            BigDecimal refunds, BigDecimal netRevenue, String currency) {}

    @GetMapping("/summary")
    public Summary summary(@PathVariable String partnerId,
            @RequestParam(defaultValue = "XAF") String currency,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        allow(partnerId, auth);
        List<LedgerEntry> entries = filtered(partnerId, currency, from, to);
        BigDecimal gross = sum(entries, LedgerType.SALE_CREDIT);
        BigDecimal commissions = sum(entries, LedgerType.PLATFORM_COMMISSION).abs();
        BigDecimal refunds = sum(entries, LedgerType.REFUND_DEBIT).abs();
        BigDecimal balance = entries.stream().map(e -> e.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(balance, gross, commissions, refunds, balance, currency);
    }

    @GetMapping("/transactions")
    public Page<LedgerEntry> transactions(@PathVariable String partnerId,
            @RequestParam(defaultValue = "XAF") String currency,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to, Pageable pageable,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        allow(partnerId, auth);
        List<LedgerEntry> all = filtered(partnerId, currency, from, to).stream()
                .sorted((a, b) -> b.occurredAt.compareTo(a.occurredAt)).toList();
        int start = Math.min((int) pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    @GetMapping("/transactions/{id}")
    public LedgerEntry transaction(@PathVariable String partnerId, @PathVariable UUID id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        allow(partnerId, auth);
        return ledger.findById(id).filter(entry -> partnerId.equals(entry.partnerId)).orElseThrow();
    }

    private List<LedgerEntry> filtered(String partnerId, String currency, Instant from, Instant to) {
        return ledger.findByPartnerIdOrderByOccurredAtAsc(partnerId).stream()
                .filter(e -> currency.equals(e.currency))
                .filter(e -> from == null || !e.occurredAt.isBefore(from))
                .filter(e -> to == null || !e.occurredAt.isAfter(to)).toList();
    }

    private BigDecimal sum(List<LedgerEntry> entries, LedgerType type) {
        return entries.stream().filter(e -> e.transactionType == type).map(e -> e.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void allow(String partnerId, String auth) {
        Map<?, ?> response = partners.get()
                .uri("/api/v1/partners/{partnerId}/staff/permissions/{permission}", partnerId, "partner:finance-view")
                .header(HttpHeaders.AUTHORIZATION, auth).retrieve().body(Map.class);
        if (response == null || !Boolean.TRUE.equals(response.get("allowed"))) {
            throw new SecurityException("Partner finance permission denied");
        }
    }
}
