package com.yeyamo_mobile.api.feed_service.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringMutedAuthorRepository extends JpaRepository<MutedAuthorEntity, UUID> {
    List<MutedAuthorEntity> findByViewerId(String viewerId);
    void deleteByViewerIdAndAuthorId(String viewerId, String authorId);
    boolean existsByViewerIdAndAuthorId(String viewerId, String authorId);
}
