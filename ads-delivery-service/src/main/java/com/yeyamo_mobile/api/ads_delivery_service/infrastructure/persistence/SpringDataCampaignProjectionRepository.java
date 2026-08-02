package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataCampaignProjectionRepository extends JpaRepository<CampaignProjectionEntity, String> {

    @Query("""
        SELECT c FROM CampaignProjectionEntity c
        WHERE c.status = 'ACTIVE'
        AND c.startAt <= :now
        AND c.endAt >= :now
        AND c.spentAmount < c.totalBudget
        AND (c.eligiblePlacements IS NULL OR c.eligiblePlacements LIKE %:placement%)
        ORDER BY c.bidAmount DESC
        """)
    List<CampaignProjectionEntity> findActiveCampaignsForPlacement(
        @Param("placement") String placement,
        @Param("now") Instant now
    );
}
