package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CopyrightClaim;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CopyrightClaimStatus;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import com.yeyamo_mobile.api.moderation_trust_service.domain.port.CopyrightClaimRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaCopyrightClaimAdapter implements CopyrightClaimRepository {

    private final SpringCopyrightClaimRepository repo;

    public JpaCopyrightClaimAdapter(SpringCopyrightClaimRepository repo) {
        this.repo = repo;
    }

    @Override
    public CopyrightClaim save(CopyrightClaim claim) {
        return repo.save(claim);
    }

    @Override
    public Optional<CopyrightClaim> findById(String id) {
        return repo.findById(id);
    }

    @Override
    public List<CopyrightClaim> findByTargetTypeAndTargetId(TargetType type, String targetId) {
        return repo.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(type, targetId);
    }

    @Override
    public List<CopyrightClaim> findByStatus(CopyrightClaimStatus status, int limit) {
        return repo.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, limit));
    }

    @Override
    public List<CopyrightClaim> findByContentCreatorId(String creatorId) {
        return repo.findByContentCreatorIdOrderByCreatedAtDesc(creatorId);
    }
}
