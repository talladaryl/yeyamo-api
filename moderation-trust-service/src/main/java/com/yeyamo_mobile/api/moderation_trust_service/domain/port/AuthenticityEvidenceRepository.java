package com.yeyamo_mobile.api.moderation_trust_service.domain.port;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityEvidence;

import java.util.List;

public interface AuthenticityEvidenceRepository {
    AuthenticityEvidence save(AuthenticityEvidence evidence);
    List<AuthenticityEvidence> findByClaimId(String claimId);
}
