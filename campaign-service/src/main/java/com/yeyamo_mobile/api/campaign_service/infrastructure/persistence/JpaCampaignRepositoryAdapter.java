package com.yeyamo_mobile.api.campaign_service.infrastructure.persistence;

import com.yeyamo_mobile.api.campaign_service.domain.model.Campaign;
import com.yeyamo_mobile.api.campaign_service.domain.model.CampaignStatus;
import com.yeyamo_mobile.api.campaign_service.domain.port.CampaignRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCampaignRepositoryAdapter implements CampaignRepository {
    
    private final SpringDataCampaignRepository repository;

    public JpaCampaignRepositoryAdapter(SpringDataCampaignRepository repository) {
        this.repository = repository;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return toDomain(repository.save(toEntity(campaign)));
    }

    @Override
    public Optional<Campaign> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Campaign> findByIdAndPartnerId(UUID id, String partnerId) {
        return repository.findByIdAndPartnerId(id, partnerId).map(this::toDomain);
    }

    @Override
    public Page<Campaign> findByPartnerId(String partnerId, Pageable pageable) {
        return repository.findByPartnerId(partnerId, pageable).map(this::toDomain);
    }

    @Override
    public Page<Campaign> findByPartnerIdAndStatus(String partnerId, CampaignStatus status, Pageable pageable) {
        return repository.findByPartnerIdAndStatus(partnerId, status, pageable).map(this::toDomain);
    }

    @Override
    public Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable).map(this::toDomain);
    }

    @Override
    public Page<Campaign> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public List<Campaign> findActiveWithBudgetRemaining() {
        return repository.findActiveWithBudgetRemaining().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Campaign> findExpiredActiveCampaigns() {
        return repository.findExpiredActiveCampaigns().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Campaign campaign) {
        repository.deleteById(campaign.getId());
    }

    @Override
    public long countByPartnerId(String partnerId) {
        return repository.countByPartnerId(partnerId);
    }

    private CampaignEntity toEntity(Campaign c) {
        CampaignEntity e = new CampaignEntity();
        e.setId(c.getId());
        e.setPartnerId(c.getPartnerId());
        e.setName(c.getName());
        e.setObjective(c.getObjective());
        e.setPromotedEntityType(c.getPromotedEntityType());
        e.setPromotedEntityId(c.getPromotedEntityId());
        e.setStatus(c.getStatus());
        e.setBillingModel(c.getBillingModel());
        e.setTotalBudget(c.getTotalBudget());
        e.setDailyBudget(c.getDailyBudget());
        e.setCurrency(c.getCurrency());
        e.setStartAt(c.getStartAt());
        e.setEndAt(c.getEndAt());
        e.setTargetConfiguration(c.getTargetConfiguration());
        e.setCreativeConfiguration(c.getCreativeConfiguration());
        e.setSpentAmount(c.getSpentAmount());
        e.setCreatedBy(c.getCreatedBy());
        e.setApprovedBy(c.getApprovedBy());
        e.setRejectionReason(c.getRejectionReason());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        e.setVersion(c.getVersion());
        return e;
    }

    private Campaign toDomain(CampaignEntity e) {
        Campaign c = new Campaign();
        c.setId(e.getId());
        c.setPartnerId(e.getPartnerId());
        c.setName(e.getName());
        c.setObjective(e.getObjective());
        c.setPromotedEntityType(e.getPromotedEntityType());
        c.setPromotedEntityId(e.getPromotedEntityId());
        c.setStatus(e.getStatus());
        c.setBillingModel(e.getBillingModel());
        c.setTotalBudget(e.getTotalBudget());
        c.setDailyBudget(e.getDailyBudget());
        c.setCurrency(e.getCurrency());
        c.setStartAt(e.getStartAt());
        c.setEndAt(e.getEndAt());
        c.setTargetConfiguration(e.getTargetConfiguration());
        c.setCreativeConfiguration(e.getCreativeConfiguration());
        c.setSpentAmount(e.getSpentAmount());
        c.setCreatedBy(e.getCreatedBy());
        c.setApprovedBy(e.getApprovedBy());
        c.setRejectionReason(e.getRejectionReason());
        c.setCreatedAt(e.getCreatedAt());
        c.setUpdatedAt(e.getUpdatedAt());
        c.setVersion(e.getVersion());
        return c;
    }
}
