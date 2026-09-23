package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "adventure_plan_days")
public class AdventurePlanDayEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false) private AdventurePlanEntity plan;
    @Column(name = "plan_date", nullable = false) private LocalDate date;
    @Column(nullable = false) private int position;
    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") private List<AdventurePlanItemEntity> items = new ArrayList<>();

    public void addItem(AdventurePlanItemEntity item) { item.setDay(this); items.add(item); }
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public AdventurePlanEntity getPlan() { return plan; } public void setPlan(AdventurePlanEntity plan) { this.plan = plan; }
    public LocalDate getDate() { return date; } public void setDate(LocalDate date) { this.date = date; }
    public int getPosition() { return position; } public void setPosition(int position) { this.position = position; }
    public List<AdventurePlanItemEntity> getItems() { return items; } public void setItems(List<AdventurePlanItemEntity> items) { this.items = items; }
}
