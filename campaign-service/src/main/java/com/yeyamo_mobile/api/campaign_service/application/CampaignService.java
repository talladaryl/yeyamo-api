package com.yeyamo_mobile.api.campaign_service.application;

import com.yeyamo_mobile.api.campaign_service.application.dto.*;
import com.yeyamo_mobile.api.campaign_service.application.exception.CampaignServiceException;
import com.yeyamo_mobile.api.campaign_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.campaign_service.domain.model.Campaign;
import com.yeyamo_mobile.api.campaign_service.domain.model.CampaignStatus;
import com.yeyamo_mobile.api.campaign_service.domain.model.TargetConfiguration;
import com.yeyamo_mobile.api.campaign_service.domain.port.CampaignRepository;
import com.yeyamo_mobile.api.campaign_service.domain.port.PartnerValidationPort;
import com.yeyamo_mobile.api.campaign_service.domain.port.PromotedEntityValidationPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CampaignService {
    
    private final CampaignRepository repository;
    private final PartnerValidationPort partnerValidation;
    private final PromotedEntityValidationPort entityValidation;
    private final OutboxPort outbox;
    
    private final Counter campaignCreatedCounter;
    private final Counter campaignSubmittedCounter;
    private final Counter campaignApprovedCounter;
    private final Counter campaignRejectedCounter;
    private final Counter campaignActiveCounter;

    public CampaignService(
            CampaignRepository repository,
            PartnerValidationPort partnerValidation,
            PromotedEntityValidationPort entityValidation,
            OutboxPort outbox,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.partnerValidation = partnerValidation;
        this.entityValidation = entityValidation;
        this.outbox = outbox;
        
        this.campaignCreatedCounter = Counter.builder("campaign_created_total")
                .description("Total number of campaigns created")
                .register(meterRegistry);
        this.campaignSubmittedCounter = Counter.builder("campaign_submitted_total")
                .description("Total number of campaigns submitted for review")
                .register(meterRegistry);
        this.campaignApprovedCounter = Counter.builder("campaign_approved_total")
                .description("Total number of campaigns approved")
                .register(meterRegistry);
        this.campaignRejectedCounter = Counter.builder("campaign_rejected_total")
                .description("Total number of campaigns rejected")
                .register(meterRegistry);
        this.campaignActiveCounter = Counter.builder("campaign_active_total")
                .description("Total number of campaigns activated")
                .register(meterRegistry);
    }

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, String partnerId, String actorId, String correlationId) {
        // Validate partner
        if (!partnerValidation.isValidPartner(partnerId)) {
            throw new CampaignServiceException("INVALID_PARTNER", "Partner is not validated", HttpStatus.BAD_REQUEST);
        }
        
        // Validate promoted entity
        if (!entityValidation.exists(request.getPromotedEntityType(), request.getPromotedEntityId())) {
            throw new CampaignServiceException("ENTITY_NOT_FOUND", "Promoted entity does not exist", HttpStatus.BAD_REQUEST);
        }
        
        Campaign campaign = Campaign.create(
                partnerId,
                request.getName(),
                request.getObjective(),
                request.getPromotedEntityType(),
                request.getPromotedEntityId(),
                request.getBillingModel(),
                request.getTotalBudget(),
                request.getDailyBudget(),
                request.getCurrency(),
                request.getStartAt(),
                request.getEndAt(),
                request.getTargetConfiguration(),
                request.getCreativeConfiguration(),
                actorId
        );
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.created", saved, actorId, correlationId);
        campaignCreatedCounter.increment();
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse updateDraftCampaign(UUID campaignId, UpdateCampaignRequest request, String partnerId, String actorId, String correlationId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        
        campaign.update(
                request.getName(),
                request.getObjective(),
                request.getBillingModel(),
                request.getTotalBudget(),
                request.getDailyBudget(),
                request.getStartAt(),
                request.getEndAt(),
                request.getTargetConfiguration(),
                request.getCreativeConfiguration()
        );
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.updated", saved, actorId, correlationId);
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse submitCampaign(UUID campaignId, String partnerId, String actorId, String correlationId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        campaign.submit();
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.submitted", saved, actorId, correlationId);
        campaignSubmittedCounter.increment();
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse approveCampaign(UUID campaignId, String approvedBy, String actorId, String correlationId) {
        Campaign campaign = repository.findById(campaignId)
                .orElseThrow(() -> notFound());
        
        campaign.approve(approvedBy);
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.approved", saved, actorId, correlationId);
        campaignApprovedCounter.increment();
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse rejectCampaign(UUID campaignId, RejectCampaignRequest request, String actorId, String correlationId) {
        Campaign campaign = repository.findById(campaignId)
                .orElseThrow(() -> notFound());
        
        campaign.reject(request.getRejectionReason());
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.rejected", saved, actorId, correlationId);
        campaignRejectedCounter.increment();
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse activateCampaign(UUID campaignId, String partnerId, String actorId, String correlationId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        campaign.activate();
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.activated", saved, actorId, correlationId);
        campaignActiveCounter.increment();
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse pauseCampaign(UUID campaignId, String partnerId, String actorId, String correlationId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        campaign.pause();
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.paused", saved, actorId, correlationId);
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse resumeCampaign(UUID campaignId, String partnerId, String actorId, String correlationId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        campaign.resume();
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.resumed", saved, actorId, correlationId);
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse cancelCampaign(UUID campaignId, String partnerId, String actorId, String correlationId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        campaign.cancel();
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.cancelled", saved, actorId, correlationId);
        
        return CampaignResponse.from(saved);
    }

    @Transactional
    public CampaignResponse completeCampaign(UUID campaignId, String partnerId, String actorId, String correlationId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        campaign.complete();
        
        Campaign saved = repository.save(campaign);
        publishEvent("campaign.completed", saved, actorId, correlationId);
        
        return CampaignResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID campaignId, String partnerId) {
        Campaign campaign = findByIdAndPartnerId(campaignId, partnerId);
        return CampaignResponse.from(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignAdmin(UUID campaignId) {
        Campaign campaign = repository.findById(campaignId)
                .orElseThrow(() -> notFound());
        return CampaignResponse.from(campaign);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> listPartnerCampaigns(String partnerId, CampaignStatus status, Pageable pageable) {
        Page<Campaign> campaigns = (status != null)
                ? repository.findByPartnerIdAndStatus(partnerId, status, pageable)
                : repository.findByPartnerId(partnerId, pageable);
        return campaigns.map(CampaignResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> listAllCampaigns(CampaignStatus status, Pageable pageable) {
        Page<Campaign> campaigns = (status != null)
                ? repository.findByStatus(status, pageable)
                : repository.findAll(pageable);
        return campaigns.map(CampaignResponse::from);
    }

    private Campaign findByIdAndPartnerId(UUID id, String partnerId) {
        return repository.findByIdAndPartnerId(id, partnerId)
                .orElseThrow(() -> notFound());
    }

    private CampaignServiceException notFound() {
        return new CampaignServiceException("CAMPAIGN_NOT_FOUND", "Campaign not found", HttpStatus.NOT_FOUND);
    }

    private void publishEvent(String eventType, Campaign campaign, String actorId, String correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignId", campaign.getId().toString());
        payload.put("partnerId", campaign.getPartnerId());
        payload.put("name", campaign.getName());
        payload.put("status", campaign.getStatus().name());
        payload.put("objective", campaign.getObjective().name());
        payload.put("promotedEntityType", campaign.getPromotedEntityType().name());
        payload.put("promotedEntityId", campaign.getPromotedEntityId());
        payload.put("billingModel", campaign.getBillingModel().name());
        payload.put("totalBudget", campaign.getTotalBudget());
        payload.put("dailyBudget", campaign.getDailyBudget());
        payload.put("spentAmount", campaign.getSpentAmount());
        payload.put("currency", campaign.getCurrency());
        payload.put("startAt", campaign.getStartAt().toString());
        payload.put("endAt", campaign.getEndAt().toString());
        payload.put("targetConfiguration", safeTarget(campaign.getTargetConfiguration()));
        payload.put("creativeConfiguration", campaign.getCreativeConfiguration());
        
        if (campaign.getApprovedBy() != null) {
            payload.put("approvedBy", campaign.getApprovedBy());
        }
        if (campaign.getRejectionReason() != null) {
            payload.put("rejectionReason", campaign.getRejectionReason());
        }
        
        outbox.append(eventType, campaign.getId(), actorId, correlationId, payload);
    }

    private Map<String, Object> safeTarget(TargetConfiguration target) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("countryCodes", target.getCountryCodes());
        safe.put("regionIds", target.getRegionIds());
        safe.put("cityIds", target.getCityIds());
        safe.put("districtIds", target.getDistrictIds());
        safe.put("minimumAge", target.getMinimumAge());
        safe.put("maximumAge", target.getMaximumAge());
        safe.put("interestIds", target.getInterestIds());
        safe.put("categoryIds", target.getCategoryIds());
        safe.put("languageCodes", target.getLanguageCodes());
        safe.put("activeDays", target.getActiveDays());
        safe.put("startHour", target.getStartHour());
        safe.put("endHour", target.getEndHour());
        safe.put("frequencyCapPerUserPerDay",
            target.getFrequencyCapPerUserPerDay());
        safe.put("frequencyCapPerUserTotal",
            target.getFrequencyCapPerUserTotal());
        return safe;
    }
}
