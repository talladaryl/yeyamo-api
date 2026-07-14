package com.yeyamo_mobile.api.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.auth_service.models.RefreshToken;
import com.yeyamo_mobile.api.auth_service.models.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);
}
