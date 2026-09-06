package com.yeyamo_mobile.api.booking_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "place_read_model")
public class PlaceReadModelEntity {

    @Id
    @Column(name = "place_id")
    private UUID placeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PlaceReadModelEntity() {}

    public PlaceReadModelEntity(UUID placeId, String name, boolean active, Instant updatedAt) {
        this.placeId = placeId;
        this.name = name;
        this.active = active;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getPlaceId() {
        return placeId;
    }

    public void setPlaceId(UUID placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
