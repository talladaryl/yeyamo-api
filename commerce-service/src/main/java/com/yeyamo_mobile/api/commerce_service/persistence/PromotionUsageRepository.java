package com.yeyamo_mobile.api.commerce_service.persistence;
import org.springframework.data.jpa.repository.*;import java.util.*;
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage,UUID>{long countByPromotionIdAndUserId(UUID p,String u);}
