package com.yeyamo_mobile.api.payment_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface RefundRepository extends JpaRepository<RefundEntity,UUID>{Optional<RefundEntity>findByIdempotencyKey(String key);Optional<RefundEntity>findByProviderRefundId(String id);List<RefundEntity>findByPaymentIdOrderByCreatedAtDesc(UUID payment);}
