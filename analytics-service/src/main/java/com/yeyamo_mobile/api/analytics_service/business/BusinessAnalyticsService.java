package com.yeyamo_mobile.api.analytics_service.business;

import com.fasterxml.jackson.databind.*;
import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class BusinessAnalyticsService {
    private static final Set<String> SUPPORTED = Set.of(
        "ad.impression.recorded", "ad.click.recorded", "ad.conversion.recorded",
        "campaign.activated", "campaign.completed", "campaign.budget.exhausted",
        "ticket.order.created", "ticket.issued", "ticket.validated",
        "ticket.scan.rejected", "ticket.refunded", "payment.confirmed",
        "commission.calculated", "settlement.completed", "promotion.applied");

    private final ObjectMapper json;
    private final AnalyticsInboxRepository inbox;
    private final StoredAnalyticsEventRepository events;
    private final ReachFingerprintRepository reach;
    private final DailyAggregateRepository aggregates;
    private final Counter processed;
    private final Counter duplicates;
    private final DistributionSummary lag;
    private final int minimumSegmentSize;

    public BusinessAnalyticsService(ObjectMapper json,
            AnalyticsInboxRepository inbox,
            StoredAnalyticsEventRepository events,
            ReachFingerprintRepository reach,
            DailyAggregateRepository aggregates, MeterRegistry meters,
            @Value("${analytics.privacy.minimum-segment-size:5}")
            int minimumSegmentSize) {
        this.json = json;
        this.inbox = inbox;
        this.events = events;
        this.reach = reach;
        this.aggregates = aggregates;
        this.processed = meters.counter("analytics.events.processed");
        this.duplicates = meters.counter("analytics.events.duplicates");
        this.lag = DistributionSummary.builder("analytics.consumer.lag.seconds")
            .publishPercentileHistogram().register(meters);
        this.minimumSegmentSize = minimumSegmentSize;
    }

    @Transactional
    public void ingest(String raw) {
        try {
            JsonNode root = json.readTree(raw);
            UUID eventId = UUID.fromString(required(root, "eventId"));
            if (inbox.existsById(eventId)) {
                duplicates.increment();
                return;
            }
            int version = root.path("eventVersion").asInt();
            if (version != 1) throw new IllegalArgumentException("Unsupported event version");
            String type = required(root, "eventType");
            Instant occurredAt = Instant.parse(required(root, "occurredAt"));
            JsonNode payload = root.path("payload");
            if (!payload.isObject()) throw new IllegalArgumentException("payload is required");

            StoredAnalyticsEvent stored = store(
                eventId, type, occurredAt, payload, raw);
            events.save(stored);
            if (SUPPORTED.contains(type)) project(stored, payload);
            inbox.save(new AnalyticsInbox(eventId, type, occurredAt));
            lag.record(Math.max(0, Duration.between(occurredAt, Instant.now()).toSeconds()));
            processed.increment();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid analytics event", exception);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"campaignAnalytics", "ticketAnalytics"}, allEntries = true)
    public long rebuild() {
        List<StoredAnalyticsEvent> replay = events.findAllByOrderByOccurredAtAscEventIdAsc();
        aggregates.clearAll();
        reach.deleteAllInBatch();
        for (StoredAnalyticsEvent stored : replay) {
            try {
                project(stored, json.readTree(stored.payload).path("payload"));
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot rebuild event " + stored.eventId, exception);
            }
        }
        return replay.size();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "campaignAnalytics",
        key = "#partnerId + ':' + #campaignId + ':' + #from + ':' + #to + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<Metrics> campaign(String partnerId, String campaignId,
            LocalDate from, LocalDate to, Pageable pageable) {
        return aggregates.findByPartnerIdAndScopeTypeAndScopeIdAndStatDateBetween(
            partnerId, "CAMPAIGN", campaignId, from, to, pageable)
            .map(row -> metrics(row, true));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ticketAnalytics",
        key = "#partnerId + ':' + #eventId + ':' + #from + ':' + #to + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<Metrics> ticketing(String partnerId, String eventId,
            LocalDate from, LocalDate to, Pageable pageable) {
        return aggregates.findByPartnerIdAndScopeTypeAndScopeIdAndStatDateBetween(
            partnerId, "TICKET_EVENT", eventId, from, to, pageable)
            .map(row -> metrics(row, true));
    }

    @Transactional(readOnly = true)
    public Page<Metrics> admin(String scopeType, String scopeId, LocalDate from,
            LocalDate to, Pageable pageable) {
        return aggregates.findByScopeTypeAndScopeIdAndStatDateBetween(
            scopeType, scopeId, from, to, pageable)
            .map(row -> metrics(row, false));
    }

    @Transactional(readOnly = true)
    public Optional<Metrics> peakEntry(String partnerId, String eventId,
            LocalDate from, LocalDate to) {
        return aggregates
            .findFirstByPartnerIdAndScopeTypeAndScopeIdAndDimensionTypeAndStatDateBetweenOrderByScansDesc(
                partnerId, "TICKET_EVENT", eventId, "ENTRY_HOUR", from, to)
            .map(row -> metrics(row, true));
    }

    private StoredAnalyticsEvent store(UUID id, String type, Instant occurred,
            JsonNode payload, String raw) {
        StoredAnalyticsEvent event = new StoredAnalyticsEvent();
        event.eventId = id;
        event.eventType = type;
        event.occurredAt = occurred;
        event.partnerId = text(payload, "partnerId", null);
        event.campaignId = text(payload, "campaignId", null);
        event.eventEntityId = first(payload, "eventId", "ticketEventId", "sourceEntityId");
        event.payload = raw;
        event.storedAt = Instant.now();
        return event;
    }

    private void project(StoredAnalyticsEvent event, JsonNode payload) {
        String scopeType = event.eventType.startsWith("ad.")
            || event.eventType.startsWith("campaign.") ? "CAMPAIGN" : "TICKET_EVENT";
        String scopeId = scopeType.equals("CAMPAIGN")
            ? first(payload, "campaignId") : first(payload, "eventId", "ticketEventId", "sourceEntityId");
        if (scopeId == null) return;
        update(event, payload, scopeType, scopeId, "TOTAL", "ALL");
        dimensions(payload).forEach((type, value) ->
            update(event, payload, scopeType, scopeId, type, value));
    }

    private void update(StoredAnalyticsEvent event, JsonNode payload,
            String scopeType, String scopeId, String dimensionType,
            String dimensionValue) {
        LocalDate date = event.occurredAt.atZone(ZoneOffset.UTC).toLocalDate();
        DailyAggregate row = aggregates.locked(date, scopeType, scopeId,
            dimensionType, dimensionValue).orElseGet(() ->
                newAggregate(date, scopeType, scopeId, event.partnerId,
                    dimensionType, dimensionValue));
        BigDecimal amount = decimal(payload, "amount", "spend", "totalAmount");
        switch (event.eventType) {
            case "ad.impression.recorded" -> {
                row.impressions++;
                if (payload.path("qualified").asBoolean(false)) row.qualifiedImpressions++;
                if (dimensionType.equals("TOTAL") && registerReach(date, scopeId, payload)) {
                    row.uniqueReach++;
                }
                row.spend = row.spend.add(decimal(payload, "cost", "spend"));
            }
            case "ad.click.recorded" -> {
                row.clicks++;
                row.spend = row.spend.add(decimal(payload, "cost", "spend"));
            }
            case "ad.conversion.recorded" -> row.conversions++;
            case "campaign.activated" -> row.budget = decimal(payload, "budget");
            case "campaign.budget.exhausted" -> row.spend = row.spend.add(amount);
            case "ticket.issued" -> {
                row.ticketsSold += Math.max(1, payload.path("quantity").asLong(1));
                // Issuance only happens after a server-confirmed payment. Using this
                // ticket-domain event keeps the revenue attributed to the event.
                row.revenue = row.revenue.add(amount);
            }
            case "ticket.validated" -> row.scans++;
            case "ticket.scan.rejected" -> row.rejectedScans++;
            case "ticket.refunded" -> row.refunds = row.refunds.add(amount);
            case "payment.confirmed" -> row.revenue = row.revenue.add(amount);
            case "commission.calculated" -> row.commission = row.commission.add(amount);
            default -> { }
        }
        row.updatedAt = Instant.now();
        aggregates.save(row);
    }

    private boolean registerReach(LocalDate date, String campaign, JsonNode payload) {
        String subject = first(payload, "anonymousId", "deviceId", "userHash", "sessionId");
        if (subject == null) return false;
        String fingerprint = sha256(date + "|" + campaign + "|" + subject);
        if (reach.existsById(fingerprint)) return false;
        ReachFingerprint value = new ReachFingerprint();
        value.fingerprint = fingerprint;
        value.statDate = date;
        value.campaignId = campaign;
        reach.save(value);
        return true;
    }

    private DailyAggregate newAggregate(LocalDate date, String scopeType,
            String scopeId, String partnerId, String dimensionType, String value) {
        DailyAggregate row = new DailyAggregate();
        row.id = UUID.randomUUID();
        row.statDate = date;
        row.scopeType = scopeType;
        row.scopeId = scopeId;
        row.partnerId = partnerId;
        row.dimensionType = dimensionType;
        row.dimensionValue = sanitizeDimension(dimensionType, value);
        row.updatedAt = Instant.now();
        return row;
    }

    private Map<String, String> dimensions(JsonNode payload) {
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "CITY", text(payload, "city", null));
        put(values, "PLACEMENT", text(payload, "placement", null));
        put(values, "DEVICE", text(payload, "deviceType", null));
        put(values, "TICKET_TYPE", text(payload, "ticketType", null));
        put(values, "GATE", text(payload, "gate", null));
        put(values, "STAFF", text(payload, "staffId", null));
        put(values, "ENTRY_HOUR", entryHour(payload));
        return values;
    }

    private Metrics metrics(DailyAggregate row, boolean applyPrivacy) {
        boolean suppressed = applyPrivacy && !row.dimensionType.equals("TOTAL")
            && sampleSize(row) < minimumSegmentSize;
        long impressions = suppressed ? 0 : row.impressions;
        long clicks = suppressed ? 0 : row.clicks;
        long conversions = suppressed ? 0 : row.conversions;
        BigDecimal spend = suppressed ? BigDecimal.ZERO : row.spend;
        return new Metrics(row.statDate, row.dimensionType,
            suppressed ? "SUPPRESSED" : row.dimensionValue,
            impressions, suppressed ? 0 : row.qualifiedImpressions,
            suppressed ? 0 : row.uniqueReach, clicks, conversions,
            suppressed ? 0 : row.ticketsSold, suppressed ? 0 : row.scans,
            suppressed ? 0 : row.rejectedScans, spend,
            suppressed ? BigDecimal.ZERO : row.budget,
            suppressed ? BigDecimal.ZERO : row.revenue,
            suppressed ? BigDecimal.ZERO : row.commission,
            suppressed ? BigDecimal.ZERO : row.refunds,
            nonNegative(row.budget.subtract(row.spend)),
            ratio(clicks, impressions, 100), ratio(conversions, clicks, 100),
            cost(spend, impressions, 1000), cost(spend, clicks, 1),
            cost(spend, conversions, 1),
            ratio(row.scans, row.ticketsSold, 100), suppressed);
    }

    private long sampleSize(DailyAggregate row) {
        return Math.max(row.impressions, Math.max(row.ticketsSold,
            row.scans + row.rejectedScans));
    }

    private BigDecimal ratio(long numerator, long denominator, int multiplier) {
        return denominator == 0 ? BigDecimal.ZERO :
            BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(multiplier))
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal cost(BigDecimal spend, long units, int multiplier) {
        return units == 0 ? BigDecimal.ZERO :
            spend.multiply(BigDecimal.valueOf(multiplier))
                .divide(BigDecimal.valueOf(units), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal decimal(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isNumber()) return value.decimalValue();
            if (value != null && value.isTextual()) {
                try { return new BigDecimal(value.asText()); } catch (NumberFormatException ignored) {}
            }
        }
        return BigDecimal.ZERO;
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field, null);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private String first(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field, null);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private String entryHour(JsonNode payload) {
        String value = text(payload, "scannedAt", null);
        if (value == null) return null;
        try { return String.format("%02d:00", Instant.parse(value).atZone(ZoneOffset.UTC).getHour()); }
        catch (RuntimeException ignored) { return null; }
    }

    private void put(Map<String, String> target, String type, String value) {
        if (value != null && !value.isBlank()) target.put(type, value);
    }

    private String sanitizeDimension(String type, String value) {
        if (type.equals("CITY")) return value.split("[,;]")[0].trim();
        if (type.equals("STAFF")) return sha256("staff|" + value).substring(0, 16);
        return value.length() > 160 ? value.substring(0, 160) : value;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    public record Metrics(LocalDate date, String dimensionType,
        String dimensionValue, long impressions, long qualifiedImpressions,
        long uniqueReach, long clicks, long conversions, long ticketsSold,
        long scans, long rejectedScans, BigDecimal spend, BigDecimal budget,
        BigDecimal revenue, BigDecimal commission, BigDecimal refunds,
        BigDecimal remainingBudget,
        BigDecimal ctr, BigDecimal conversionRate, BigDecimal cpm,
        BigDecimal cpc, BigDecimal cpa, BigDecimal attendanceRate,
        boolean suppressed) {}
}
