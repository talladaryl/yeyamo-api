package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdventurePlanRepository extends JpaRepository<AdventurePlanEntity, UUID> {
    Page<AdventurePlanEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @EntityGraph(attributePaths = { "days", "days.items" })
    @Query("select p from AdventurePlanEntity p where p.id = :id and p.userId = :userId")
    Optional<AdventurePlanEntity> findDetailByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);
}
