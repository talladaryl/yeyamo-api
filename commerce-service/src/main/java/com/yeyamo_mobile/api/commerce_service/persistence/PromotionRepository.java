package com.yeyamo_mobile.api.commerce_service.persistence;
import com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.PromotionStatus;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.*;import jakarta.persistence.LockModeType;import java.util.*;
public interface PromotionRepository extends JpaRepository<Promotion,UUID>,JpaSpecificationExecutor<Promotion>{
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select p from Promotion p where upper(p.code)=upper(:code)")Optional<Promotion>lockedByCode(@Param("code")String code);
 Optional<Promotion>findByIdempotencyKey(String key);
 org.springframework.data.domain.Page<Promotion>findByStatus(PromotionStatus status,org.springframework.data.domain.Pageable pageable);
 org.springframework.data.domain.Page<Promotion>findByPartnerId(String partnerId,org.springframework.data.domain.Pageable pageable);
 org.springframework.data.domain.Page<Promotion>findByPartnerIdAndStatus(String partnerId,PromotionStatus status,org.springframework.data.domain.Pageable pageable);
}
