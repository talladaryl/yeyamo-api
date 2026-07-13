package com.yeyamo_mobile.api.user_service.domain.port;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;

public interface UserProfileRepository {
    UserProfile save(UserProfile profile);
    Optional<UserProfile> findById(UUID id);
    Optional<UserProfile> findByAuthUserId(String authUserId);
    boolean existsByAuthUserId(String authUserId);
    Page<UserProfile> searchPublic(String query, Pageable pageable);
}
