package com.yeyamo_mobile.api.campaign_service.infrastructure.persistence;

import com.yeyamo_mobile.api.campaign_service.domain.model.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCampaignRepository extends JpaRepository<CampaignEntity, UUID>, JpaSpecificationExecutor<CampaignEntity> {
    
    Optional<CampaignEntity> findByIdAndPartnerId(UUID id, String partnerId);
    
    Page<CampaignEntity> findByPartnerId(String partnerId, Pageable pageable);
    
    Page<CampaignEntity> findByPartnerIdAndStatus(String partnerId, CampaignStatus status, Pageable pageable);
    
    @Query("SELECT c FROM CampaignEntity c WHERE c.status = :status")
    Page<CampaignEntity> findByStatus(@Param("status") CampaignStatus status, Pageable pageable);
    
    @Query("SELECT c FROM CampaignEntity c WHERE c.status = 'ACTIVE' AND c.spentAmount < c.totalBudget")
    List<CampaignEntity> findActiveWithBudgetRemaining();
    
    @Query("SELECT c FROM CampaignEntity c WHERE c.status = 'ACTIVE' AND c.endAt < CURRENT_TIMESTAMP")
    List<CampaignEntity> findExpiredActiveCampaigns();
    
    long countByPartnerId(String partnerId);
}
