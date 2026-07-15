package com.yeyamo_mobile.api.referral_service.infrastructure.persistence;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface ReferralHistoryRepository extends JpaRepository<ReferralHistoryEntity,UUID>{List<ReferralHistoryEntity>findByAttributionIdOrderByOccurredAtAsc(UUID attribution);}
