package com.yeyamo_mobile.api.analytics_service.business;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "analytics_daily_aggregates",
    uniqueConstraints = @UniqueConstraint(columnNames = {
        "stat_date", "scope_type", "scope_id", "dimension_type", "dimension_value"
    }))
public class DailyAggregate {
    @Id public UUID id;
    public LocalDate statDate;
    public String scopeType;
    public String scopeId;
    public String partnerId;
    public String dimensionType;
    public String dimensionValue;
    public long impressions;
    public long qualifiedImpressions;
    public long uniqueReach;
    public long clicks;
    public long conversions;
    public long ticketsSold;
    public long scans;
    public long rejectedScans;
    public BigDecimal spend = BigDecimal.ZERO;
    public BigDecimal budget = BigDecimal.ZERO;
    public BigDecimal revenue = BigDecimal.ZERO;
    public BigDecimal commission = BigDecimal.ZERO;
    public BigDecimal refunds = BigDecimal.ZERO;
    public Instant updatedAt;
}
