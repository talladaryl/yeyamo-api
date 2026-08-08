package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.SensitiveContentFlag;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import com.yeyamo_mobile.api.moderation_trust_service.domain.port.SensitiveContentFlagRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaSensitiveContentFlagAdapter implements SensitiveContentFlagRepository {

    private final SpringSensitiveContentFlagRepository repo;

    public JpaSensitiveContentFlagAdapter(SpringSensitiveContentFlagRepository repo) {
        this.repo = repo;
    }

    @Override
    public SensitiveContentFlag save(SensitiveContentFlag flag) {
        SensitiveContentFlagEntity entity = toEntity(flag);
        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<SensitiveContentFlag> findById(String id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public boolean hasApprovedFlag(TargetType targetType, String targetId) {
        return repo.existsByTargetTypeAndTargetIdAndStatus(
                targetType, targetId, SensitiveContentFlag.FlagStatus.APPROVED);
    }

    @Override
    public List<SensitiveContentFlag> findByTarget(TargetType targetType, String targetId) {
        return repo.findByTargetTypeAndTargetId(targetType, targetId)
                   .stream().map(this::toDomain).toList();
    }

    // ---- Mapping -----------------------------------------------------------

    private SensitiveContentFlagEntity toEntity(SensitiveContentFlag f) {
        SensitiveContentFlagEntity e = new SensitiveContentFlagEntity();
        e.setId(f.getId());
        e.setTargetType(f.getTargetType());
        e.setTargetId(f.getTargetId());
        e.setFlagType(f.getFlagType());
        e.setReason(f.getReason());
        e.setFlaggedBy(f.getFlaggedBy());
        e.setApprovedBy(f.getApprovedBy());
        e.setStatus(f.getStatus());
        e.setCreatedAt(f.getCreatedAt());
        e.setApprovedAt(f.getApprovedAt());
        return e;
    }

    private SensitiveContentFlag toDomain(SensitiveContentFlagEntity e) {
        SensitiveContentFlag f = new SensitiveContentFlag();
        f.setId(e.getId());
        f.setTargetType(e.getTargetType());
        f.setTargetId(e.getTargetId());
        f.setFlagType(e.getFlagType());
        f.setReason(e.getReason());
        f.setFlaggedBy(e.getFlaggedBy());
        f.setApprovedBy(e.getApprovedBy());
        f.setStatus(e.getStatus());
        f.setCreatedAt(e.getCreatedAt());
        f.setApprovedAt(e.getApprovedAt());
        return f;
    }
}
