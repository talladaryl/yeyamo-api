package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventureBudgetTier;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventurePartyType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "adventure_plans")
public class AdventurePlanEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false, length = 120) private String userId;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode;
    @Column(name = "country_timezone", nullable = false, length = 80) private String countryTimezone;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Enumerated(EnumType.STRING) @Column(name = "party_type", nullable = false, length = 20) private AdventurePartyType partyType;
    @Enumerated(EnumType.STRING) @Column(name = "budget_tier", length = 20) private AdventureBudgetTier budgetTier;
    @Column(name = "budget_minimum", precision = 19, scale = 2) private BigDecimal budgetMinimum;
    @Column(name = "budget_maximum", precision = 19, scale = 2) private BigDecimal budgetMaximum;
    @Column(name = "currency_code", length = 3) private String currencyCode;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "adventure_plan_interests", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "interest_code", nullable = false, length = 100)
    private Set<String> interestCodes = new LinkedHashSet<>();
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AdventurePlanDayEntity> days = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    public void addDay(AdventurePlanDayEntity day) { day.setPlan(this); days.add(day); }
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; } public void setUserId(String userId) { this.userId = userId; }
    public String getCountryCode() { return countryCode; } public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getCountryTimezone() { return countryTimezone; } public void setCountryTimezone(String value) { countryTimezone = value; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalTime getStartTime() { return startTime; } public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; } public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public AdventurePartyType getPartyType() { return partyType; } public void setPartyType(AdventurePartyType partyType) { this.partyType = partyType; }
    public AdventureBudgetTier getBudgetTier() { return budgetTier; } public void setBudgetTier(AdventureBudgetTier budgetTier) { this.budgetTier = budgetTier; }
    public BigDecimal getBudgetMinimum() { return budgetMinimum; } public void setBudgetMinimum(BigDecimal value) { budgetMinimum = value; }
    public BigDecimal getBudgetMaximum() { return budgetMaximum; } public void setBudgetMaximum(BigDecimal value) { budgetMaximum = value; }
    public String getCurrencyCode() { return currencyCode; } public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public Set<String> getInterestCodes() { return interestCodes; } public void setInterestCodes(Set<String> values) { interestCodes = values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values); }
    public List<AdventurePlanDayEntity> getDays() { return days; } public void setDays(List<AdventurePlanDayEntity> days) { this.days = days; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
