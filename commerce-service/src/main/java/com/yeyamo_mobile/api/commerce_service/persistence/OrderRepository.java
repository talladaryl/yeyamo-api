package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.*;import jakarta.persistence.LockModeType;import java.util.*;
public interface OrderRepository extends JpaRepository<CommerceOrder,UUID>{Optional<CommerceOrder>findByIdempotencyKey(String k);List<CommerceOrder>findByUserIdOrderByCreatedAtDesc(String u);@Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select o from CommerceOrder o where o.id=:id")Optional<CommerceOrder>locked(@Param("id")UUID id);}
