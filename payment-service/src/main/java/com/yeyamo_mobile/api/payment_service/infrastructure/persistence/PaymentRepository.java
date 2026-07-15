package com.yeyamo_mobile.api.payment_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;
public interface PaymentRepository extends JpaRepository<PaymentEntity,UUID>{Optional<PaymentEntity>findByBookingId(UUID id);Optional<PaymentEntity>findByIdempotencyKey(String key);Optional<PaymentEntity>findByProviderPaymentId(String id);List<PaymentEntity>findByUserIdOrderByCreatedAtDesc(String user);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select p from PaymentEntity p where p.bookingId=:bookingId")Optional<PaymentEntity>findByBookingIdLocked(UUID bookingId);}
