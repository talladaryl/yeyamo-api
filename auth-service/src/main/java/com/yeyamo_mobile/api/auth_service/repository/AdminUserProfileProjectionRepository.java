package com.yeyamo_mobile.api.auth_service.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.auth_service.models.AdminUserProfileProjection;

public interface AdminUserProfileProjectionRepository extends JpaRepository<AdminUserProfileProjection, Long> {
    List<AdminUserProfileProjection> findByAuthUserIdIn(Collection<Long> userIds);
}
