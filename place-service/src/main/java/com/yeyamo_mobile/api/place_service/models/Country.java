package com.yeyamo_mobile.api.place_service.models;

import com.yeyamo_mobile.api.place_service.enums.CountryLaunchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "countries")
public class Country {
    @Id
    @Column(length = 2)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "launch_status", nullable = false, length = 20)
    private CountryLaunchStatus launchStatus;
    @Column(name = "default_currency_code", nullable = false, length = 3)
    private String defaultCurrencyCode;
    @Column(name = "default_timezone", nullable = false, length = 80)
    private String defaultTimezone;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getCode() { return code; }
    public String getName() { return name; }
    public CountryLaunchStatus getLaunchStatus() { return launchStatus; }
    public String getDefaultCurrencyCode() { return defaultCurrencyCode; }
    public String getDefaultTimezone() { return defaultTimezone; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
