package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeyamo_mobile.api.user_service.domain.model.ProfileStatus;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;

public interface SpringDataUserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByAuthUserId(String authUserId);
    boolean existsByAuthUserId(String authUserId);

    @Query("""
            select p from UserProfileEntity p
            where p.status = :status and p.visibility = :visibility
              and (:query = '' or lower(p.displayName) like lower(concat('%', :query, '%')))
            """)
    Page<UserProfileEntity> searchPublic(@Param("query") String query,
            @Param("status") ProfileStatus status,
            @Param("visibility") ProfileVisibility visibility,
            Pageable pageable);
}
