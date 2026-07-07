package com.yeyamo_mobile.api.admin_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;
import com.yeyamo_mobile.api.admin_service.models.PartnerValidation;

public interface PartnerValidationRepository extends JpaRepository<PartnerValidation, UUID> {
    List<PartnerValidation> findByStatusOrderByCreatedAtDesc(ValidationStatus status);
    List<PartnerValidation> findAllByOrderByCreatedAtDesc();
}
