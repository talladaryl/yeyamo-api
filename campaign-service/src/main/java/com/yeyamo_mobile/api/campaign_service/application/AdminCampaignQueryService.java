package com.yeyamo_mobile.api.campaign_service.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.campaign_service.application.dto.CampaignResponse;
import com.yeyamo_mobile.api.campaign_service.application.exception.CampaignServiceException;
import com.yeyamo_mobile.api.campaign_service.domain.model.*;
import com.yeyamo_mobile.api.campaign_service.infrastructure.outbox.*;
import com.yeyamo_mobile.api.campaign_service.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;

@Service
public class AdminCampaignQueryService {
    private final SpringDataCampaignRepository campaigns;
    private final OutboxEventRepository audit;
    public AdminCampaignQueryService(SpringDataCampaignRepository campaigns,OutboxEventRepository audit){this.campaigns=campaigns;this.audit=audit;}

    @Transactional(readOnly=true)
    public Page<CampaignResponse> list(String search,String advertiserId,CampaignStatus status,
            CampaignObjective type,Instant createdFrom,Instant createdTo,
            Instant startFrom,Instant startTo,Pageable pageable){
        Specification<CampaignEntity> spec=(root,query,builder)->builder.conjunction();
        if(has(search))spec=spec.and((root,query,builder)->builder.like(builder.lower(root.get("name")),"%"+search.toLowerCase(Locale.ROOT)+"%"));
        if(has(advertiserId))spec=spec.and((root,query,builder)->builder.equal(root.get("partnerId"),advertiserId));
        if(status!=null)spec=spec.and((root,query,builder)->builder.equal(root.get("status"),status));
        if(type!=null)spec=spec.and((root,query,builder)->builder.equal(root.get("objective"),type));
        if(createdFrom!=null)spec=spec.and((root,query,builder)->builder.greaterThanOrEqualTo(root.get("createdAt"),createdFrom));
        if(createdTo!=null)spec=spec.and((root,query,builder)->builder.lessThanOrEqualTo(root.get("createdAt"),createdTo));
        if(startFrom!=null)spec=spec.and((root,query,builder)->builder.greaterThanOrEqualTo(root.get("startAt"),startFrom));
        if(startTo!=null)spec=spec.and((root,query,builder)->builder.lessThanOrEqualTo(root.get("startAt"),startTo));
        return campaigns.findAll(spec,pageable).map(this::response);
    }
    @Transactional(readOnly=true)
    public AdminCampaignDetail detail(UUID id){
        CampaignEntity entity=campaigns.findById(id).orElseThrow(()->new CampaignServiceException("CAMPAIGN_NOT_FOUND","Campaign not found",HttpStatus.NOT_FOUND));
        CampaignResponse campaign=response(entity);
        List<AuditEntry> entries=audit.findByAggregateIdOrderByOccurredAtAsc(id.toString()).stream()
                .filter(event->event.getEventType().startsWith("campaign."))
                .map(event->new AuditEntry(event.getEventType(),event.getOccurredAt(),event.getPayload())).toList();
        return new AdminCampaignDetail(entity.getPartnerId(),campaign,entity.getCreativeConfiguration(),
                entity.getTargetConfiguration(),new Budget(entity.getTotalBudget(),entity.getDailyBudget(),
                entity.getSpentAmount(),entity.getCurrency()),new Schedule(entity.getStartAt(),entity.getEndAt()),
                null,entries);
    }
    private CampaignResponse response(CampaignEntity entity){
        CampaignResponse value=new CampaignResponse();value.setId(entity.getId());value.setPartnerId(entity.getPartnerId());value.setName(entity.getName());value.setObjective(entity.getObjective());value.setPromotedEntityType(entity.getPromotedEntityType());value.setPromotedEntityId(entity.getPromotedEntityId());value.setStatus(entity.getStatus());value.setBillingModel(entity.getBillingModel());value.setTotalBudget(entity.getTotalBudget());value.setDailyBudget(entity.getDailyBudget());value.setCurrency(entity.getCurrency());value.setStartAt(entity.getStartAt());value.setEndAt(entity.getEndAt());value.setTargetConfiguration(entity.getTargetConfiguration());value.setCreativeConfiguration(entity.getCreativeConfiguration());value.setSpentAmount(entity.getSpentAmount());value.setCreatedBy(entity.getCreatedBy());value.setApprovedBy(entity.getApprovedBy());value.setRejectionReason(entity.getRejectionReason());value.setCreatedAt(entity.getCreatedAt());value.setUpdatedAt(entity.getUpdatedAt());value.setVersion(entity.getVersion());return value;
    }
    private boolean has(String value){return value!=null&&!value.isBlank();}
    public record Budget(BigDecimal total,BigDecimal daily,BigDecimal spent,String currency){}
    public record Schedule(Instant startAt,Instant endAt){}
    public record AuditEntry(String action,Instant occurredAt,String metadata){}
    public record AdminCampaignDetail(String advertiserId,CampaignResponse campaign,Object creatives,
            Object target,Budget budget,Schedule schedule,Object performanceSummary,List<AuditEntry> audit){}
}
