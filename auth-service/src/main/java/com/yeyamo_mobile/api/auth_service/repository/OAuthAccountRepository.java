package com.yeyamo_mobile.api.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.auth_service.models.OAuthAccount;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
