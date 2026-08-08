package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityEvidence;
import com.yeyamo_mobile.api.moderation_trust_service.domain.port.AuthenticityEvidenceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaAuthenticityEvidenceAdapter implements AuthenticityEvidenceRepository {

    private final SpringAuthenticityEvidenceRepository repo;

    public JpaAuthenticityEvidenceAdapter(SpringAuthenticityEvidenceRepository repo) {
        this.repo = repo;
    }

    @Override
    public AuthenticityEvidence save(AuthenticityEvidence evidence) {
        return repo.save(evidence);
    }

    @Override
    public List<AuthenticityEvidence> findByClaimId(String claimId) {
        return repo.findByClaimIdOrderByUploadedAtDesc(claimId);
    }
}
