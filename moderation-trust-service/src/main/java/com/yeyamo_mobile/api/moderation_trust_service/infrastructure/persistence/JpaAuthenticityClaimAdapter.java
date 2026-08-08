package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityClaim;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityStatus;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import com.yeyamo_mobile.api.moderation_trust_service.domain.port.AuthenticityClaimRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaAuthenticityClaimAdapter implements AuthenticityClaimRepository {

    private final SpringAuthenticityClaimRepository repo;

    public JpaAuthenticityClaimAdapter(SpringAuthenticityClaimRepository repo) {
        this.repo = repo;
    }

    @Override
    public AuthenticityClaim save(AuthenticityClaim claim) {
        return repo.save(claim);
    }

    @Override
    public Optional<AuthenticityClaim> findById(String id) {
        return repo.findById(id);
    }

    @Override
    public Optional<AuthenticityClaim> findByTargetTypeAndTargetId(TargetType type, String targetId) {
        return repo.findByTargetTypeAndTargetId(type, targetId);
    }

    @Override
    public List<AuthenticityClaim> findByStatus(AuthenticityStatus status, int limit) {
        return repo.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(0, limit));
    }

    @Override
    public List<AuthenticityClaim> findByAssignedReviewerId(String reviewerId) {
        return repo.findByAssignedReviewerIdOrderByCreatedAtAsc(reviewerId);
    }

    @Override
    public boolean existsByTargetTypeAndTargetId(TargetType type, String targetId) {
        return repo.existsByTargetTypeAndTargetId(type, targetId);
    }
}
