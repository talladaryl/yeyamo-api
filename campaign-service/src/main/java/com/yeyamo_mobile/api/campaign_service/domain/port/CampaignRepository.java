package com.yeyamo_mobile.api.campaign_service.domain.port;

import com.yeyamo_mobile.api.campaign_service.domain.model.Campaign;
import com.yeyamo_mobile.api.campaign_service.domain.model.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository port (interface)
 * No Spring/JPA dependencies
 */
public interface CampaignRepository {
    
    Campaign save(Campaign campaign);
    
    Optional<Campaign> findById(UUID id);
    
    Optional<Campaign> findByIdAndPartnerId(UUID id, String partnerId);
    
    Page<Campaign> findByPartnerId(String partnerId, Pageable pageable);
    
    Page<Campaign> findByPartnerIdAndStatus(String partnerId, CampaignStatus status, Pageable pageable);
    
    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);
    
    Page<Campaign> findAll(Pageable pageable);
    
    List<Campaign> findActiveWithBudgetRemaining();
    
    List<Campaign> findExpiredActiveCampaigns();
    
    void delete(Campaign campaign);
    
    long countByPartnerId(String partnerId);
}
