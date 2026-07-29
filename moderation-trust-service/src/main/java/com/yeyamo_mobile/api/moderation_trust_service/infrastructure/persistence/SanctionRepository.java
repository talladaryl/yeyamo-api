package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionRepository extends JpaRepository<SanctionEntity,UUID> {
    Page<SanctionEntity> findBySubjectIdOrderByCreatedAtDesc(String subjectId,Pageable pageable);
}
