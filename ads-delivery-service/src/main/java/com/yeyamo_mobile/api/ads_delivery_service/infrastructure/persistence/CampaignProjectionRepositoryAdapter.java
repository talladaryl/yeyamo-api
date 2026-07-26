package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.CampaignProjection;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.PlacementType;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.PromotedEntityType;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.CampaignProjectionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CampaignProjectionRepositoryAdapter implements CampaignProjectionRepository {

    private final SpringDataCampaignProjectionRepository springRepository;

    public CampaignProjectionRepositoryAdapter(SpringDataCampaignProjectionRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignProjection> findActiveCampaignsForPlacement(PlacementType placement, Instant now) {
        List<CampaignProjectionEntity> entities = springRepository
            .findActiveCampaignsForPlacement(placement.name(), now);
        
        return entities.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignProjection> findById(String campaignId) {
        return springRepository.findById(campaignId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(CampaignProjection projection) {
        CampaignProjectionEntity entity = toEntity(projection);
        springRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateSpentAmount(String campaignId, BigDecimal newSpentAmount) {
        springRepository.findById(campaignId).ifPresent(entity -> {
            entity.setSpentAmount(newSpentAmount);
            entity.setUpdatedAt(Instant.now());
            springRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void deactivate(String campaignId) {
        springRepository.findById(campaignId).ifPresent(entity -> {
            entity.setStatus("INACTIVE");
            entity.setUpdatedAt(Instant.now());
            springRepository.save(entity);
        });
    }

    private CampaignProjection toDomain(CampaignProjectionEntity entity) {
        return CampaignProjection.builder()
            .campaignId(entity.getCampaignId())
            .partnerId(entity.getPartnerId())
            .name(entity.getName())
            .objective(entity.getObjective())
            .promotedEntityType(PromotedEntityType.valueOf(entity.getPromotedEntityType()))
            .promotedEntityId(entity.getPromotedEntityId())
            .billingModel(entity.getBillingModel())
            .bidAmount(entity.getBidAmount())
            .totalBudget(entity.getTotalBudget())
            .dailyBudget(entity.getDailyBudget())
            .spentAmount(entity.getSpentAmount())
            .currency(entity.getCurrency())
            .startAt(entity.getStartAt())
            .endAt(entity.getEndAt())
            .eligiblePlacements(parseList(entity.getEligiblePlacements(), PlacementType.class))
            .targetCountries(parseStringList(entity.getTargetCountries()))
            .targetRegions(parseStringList(entity.getTargetRegions()))
            .targetCities(parseStringList(entity.getTargetCities()))
            .targetInterests(parseStringList(entity.getTargetInterests()))
            .targetCategories(parseStringList(entity.getTargetCategories()))
            .minAge(entity.getMinAge())
            .maxAge(entity.getMaxAge())
            .targetLanguages(parseStringList(entity.getTargetLanguages()))
            .creativeJson(entity.getCreativeJson())
            .qualityScore(entity.getQualityScore())
            .createdAt(entity.getCreatedAt())
            .deliveryPolicyVersion(entity.getDeliveryPolicyVersion())
            .build();
    }

    private CampaignProjectionEntity toEntity(CampaignProjection domain) {
        CampaignProjectionEntity entity = new CampaignProjectionEntity();
        entity.setCampaignId(domain.getCampaignId());
        entity.setPartnerId(domain.getPartnerId());
        entity.setName(domain.getName());
        entity.setObjective(domain.getObjective());
        entity.setPromotedEntityType(domain.getPromotedEntityType().name());
        entity.setPromotedEntityId(domain.getPromotedEntityId());
        entity.setStatus("ACTIVE");
        entity.setBillingModel(domain.getBillingModel());
        entity.setBidAmount(domain.getBidAmount());
        entity.setTotalBudget(domain.getTotalBudget());
        entity.setDailyBudget(domain.getDailyBudget());
        entity.setSpentAmount(domain.getSpentAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setStartAt(domain.getStartAt());
        entity.setEndAt(domain.getEndAt());
        entity.setEligiblePlacements(serializeList(domain.getEligiblePlacements()));
        entity.setTargetCountries(serializeStringList(domain.getTargetCountries()));
        entity.setTargetRegions(serializeStringList(domain.getTargetRegions()));
        entity.setTargetCities(serializeStringList(domain.getTargetCities()));
        entity.setTargetInterests(serializeStringList(domain.getTargetInterests()));
        entity.setTargetCategories(serializeStringList(domain.getTargetCategories()));
        entity.setMinAge(domain.getMinAge());
        entity.setMaxAge(domain.getMaxAge());
        entity.setTargetLanguages(serializeStringList(domain.getTargetLanguages()));
        entity.setCreativeJson(domain.getCreativeJson());
        entity.setQualityScore(domain.getQualityScore());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(Instant.now());
        entity.setDeliveryPolicyVersion(domain.getDeliveryPolicyVersion());
        return entity;
    }

    private <T extends Enum<T>> List<T> parseList(String value, Class<T> enumClass) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> Enum.valueOf(enumClass, s))
            .collect(Collectors.toList());
    }

    private List<String> parseStringList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private <T extends Enum<T>> String serializeList(List<T> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
            .map(Enum::name)
            .collect(Collectors.joining(","));
    }

    private String serializeStringList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",", list);
    }
}
