package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.*;import jakarta.persistence.LockModeType;import java.util.*;
public interface PromotionRepository extends JpaRepository<Promotion,UUID>{@Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select p from Promotion p where upper(p.code)=upper(:code)")Optional<Promotion>lockedByCode(@Param("code")String code);}
