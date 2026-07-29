package com.yeyamo_mobile.api.payment_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;
public interface PaymentRepository extends JpaRepository<PaymentEntity,UUID>,JpaSpecificationExecutor<PaymentEntity>{Optional<PaymentEntity>findByBookingId(UUID id);Optional<PaymentEntity>findByIdempotencyKey(String key);Optional<PaymentEntity>findByProviderPaymentId(String id);List<PaymentEntity>findByUserIdOrderByCreatedAtDesc(String user);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select p from PaymentEntity p where p.bookingId=:bookingId")Optional<PaymentEntity>findByBookingIdLocked(UUID bookingId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select p from PaymentEntity p where p.id=:id")Optional<PaymentEntity>findLockedById(UUID id);
 @Query(value="SELECT provider_payment_id FROM payments WHERE provider_payment_id IS NOT NULL GROUP BY provider_payment_id HAVING COUNT(*)>1",nativeQuery=true)List<String>duplicateProviderTransactions();}
