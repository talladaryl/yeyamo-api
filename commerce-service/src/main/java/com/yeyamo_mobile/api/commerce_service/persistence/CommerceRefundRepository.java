package com.yeyamo_mobile.api.commerce_service.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface CommerceRefundRepository
        extends JpaRepository<CommerceRefund, UUID> {
    Optional<CommerceRefund> findByIdempotencyKey(String key);

    @Query("""
        select coalesce(sum(r.amount), 0)
        from CommerceRefund r
        where r.orderId = :orderId
          and r.status in ('REQUESTED', 'COMPLETED')
        """)
    BigDecimal reserved(@Param("orderId") UUID orderId);

    @Query("""
        select coalesce(sum(r.amount), 0)
        from CommerceRefund r
        where r.orderId = :orderId and r.status = 'COMPLETED'
        """)
    BigDecimal completed(@Param("orderId") UUID orderId);
}
