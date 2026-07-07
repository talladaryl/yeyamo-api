package com.yeyamo_mobile.api.admin_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;
import com.yeyamo_mobile.api.admin_service.models.PlaceValidation;

public interface PlaceValidationRepository extends JpaRepository<PlaceValidation, UUID> {
    List<PlaceValidation> findByStatusOrderByCreatedAtDesc(ValidationStatus status);
    List<PlaceValidation> findAllByOrderByCreatedAtDesc();
}
