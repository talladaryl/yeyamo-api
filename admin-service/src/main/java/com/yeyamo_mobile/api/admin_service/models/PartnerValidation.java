package com.yeyamo_mobile.api.admin_service.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "partner_validations")
@Getter
@Setter
public class PartnerValidation {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "requester_id")
    private UUID requesterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ValidationStatus status = ValidationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kyc_document_urls", nullable = false, columnDefinition = "jsonb")
    private List<String> kycDocumentUrls = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kyc_document_types", nullable = false, columnDefinition = "jsonb")
    private List<String> kycDocumentTypes = new ArrayList<>();

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
