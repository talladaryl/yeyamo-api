package com.yeyamo_mobile.api.payment_service.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, UUID> {
    Optional<PaymentAttemptEntity> findByReference(String reference);
    Optional<PaymentAttemptEntity> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentAttemptEntity> findBySourceServiceAndSourceId(String sourceService, UUID sourceId);
}
