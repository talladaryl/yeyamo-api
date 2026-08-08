package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CulturalReviewer;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.ReviewerRole;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import com.yeyamo_mobile.api.moderation_trust_service.domain.port.CulturalReviewerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaCulturalReviewerAdapter implements CulturalReviewerRepository {

    private final SpringCulturalReviewerRepository repo;

    public JpaCulturalReviewerAdapter(SpringCulturalReviewerRepository repo) {
        this.repo = repo;
    }

    @Override
    public CulturalReviewer save(CulturalReviewer reviewer) {
        return repo.save(reviewer);
    }

    @Override
    public Optional<CulturalReviewer> findById(String id) {
        return repo.findById(id);
    }

    @Override
    public Optional<CulturalReviewer> findByUserId(String userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public List<CulturalReviewer> findActiveByRole(ReviewerRole role) {
        return repo.findByRoleAndStatus(role, "ACTIVE");
    }

    @Override
    public List<CulturalReviewer> findEligibleReviewers(TargetType contentType, int limit) {
        return repo.findEligibleReviewers(contentType, PageRequest.of(0, limit));
    }
}
