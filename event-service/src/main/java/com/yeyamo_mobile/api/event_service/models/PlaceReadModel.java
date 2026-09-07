package com.yeyamo_mobile.api.event_service.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_place_read_model")
public class PlaceReadModel {
    @Id
    @Column(name = "place_id")
    private UUID placeId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaceReadModel() {}

    public PlaceReadModel(UUID placeId, String name, boolean active, Instant updatedAt) {
        this.placeId = placeId;
        this.name = name;
        this.active = active;
        this.updatedAt = updatedAt;
    }

    public UUID getPlaceId() { return placeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
