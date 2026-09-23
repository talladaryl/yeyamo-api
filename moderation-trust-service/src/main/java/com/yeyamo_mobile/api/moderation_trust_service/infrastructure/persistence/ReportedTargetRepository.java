package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportedTargetRepository extends JpaRepository<ReportedTargetEntity, String> { }
