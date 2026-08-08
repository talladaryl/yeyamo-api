package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringAuthenticityEvidenceRepository extends JpaRepository<AuthenticityEvidence, String> {
    List<AuthenticityEvidence> findByClaimIdOrderByUploadedAtDesc(String claimId);
}
