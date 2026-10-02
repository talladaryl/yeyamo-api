package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringPostViewRepository extends JpaRepository<PostViewEntity, PostViewEntity.PostViewId> {
    long countByIdPostId(UUID postId);

    @Query("select view.id.postId, count(view) from PostViewEntity view where view.id.postId in :postIds group by view.id.postId")
    List<Object[]> countByPostIds(@Param("postIds") Collection<UUID> postIds);
}
