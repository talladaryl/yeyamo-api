package com.yeyamo_mobile.api.place_service.models;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "place_partner_read_model")
public class PartnerReadModel {
    @Id private UUID partnerId;
    @Column(name = "owner_user_id", nullable = false) private String ownerUserId;
    @Column(nullable = false) private String status;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected PartnerReadModel() {}
    public PartnerReadModel(UUID partnerId, String ownerUserId, String status, Instant updatedAt) { this.partnerId=partnerId; this.ownerUserId=ownerUserId; this.status=status; this.updatedAt=updatedAt; }
    public UUID getPartnerId() { return partnerId; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String value) { ownerUserId=value; }
    public void setStatus(String value) { status=value; }
    public void setUpdatedAt(Instant value) { updatedAt=value; }
    public boolean isApproved() { return "APPROVED".equals(status); }
}
