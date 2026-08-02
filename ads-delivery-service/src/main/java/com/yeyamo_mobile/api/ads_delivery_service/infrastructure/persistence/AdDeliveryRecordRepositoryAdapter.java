package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.AdDeliveryRecord;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.AdDeliveryRecordRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class AdDeliveryRecordRepositoryAdapter implements AdDeliveryRecordRepository {

    private final SpringDataAdDeliveryRecordRepository springRepository;

    public AdDeliveryRecordRepositoryAdapter(SpringDataAdDeliveryRecordRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    @Transactional
    public void save(AdDeliveryRecord record) {
        AdDeliveryRecordEntity entity = toEntity(record);
        springRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdDeliveryRecord> findByDeliveryId(String deliveryId) {
        return springRepository.findById(deliveryId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByDeliveryId(String deliveryId) {
        return springRepository.existsByDeliveryId(deliveryId);
    }

    private AdDeliveryRecord toDomain(AdDeliveryRecordEntity entity) {
        AdDeliveryRecord record = AdDeliveryRecord.createImpression(
            entity.getDeliveryId(),
            entity.getCampaignId(),
            entity.getUserId(),
            entity.getImpressionAt(),
            entity.getViewDurationMs()
        );

        if (entity.getClickedAt() != null) {
            record.recordClick(entity.getClickedAt());
        }

        if (entity.getConvertedAt() != null) {
            record.recordConversion(
                entity.getConvertedAt(),
                entity.getConversionType(),
                entity.getConversionValue()
            );
        }

        return record;
    }

    private AdDeliveryRecordEntity toEntity(AdDeliveryRecord domain) {
        AdDeliveryRecordEntity entity = new AdDeliveryRecordEntity();
        entity.setDeliveryId(domain.getDeliveryId());
        entity.setCampaignId(domain.getCampaignId());
        entity.setUserId(domain.getUserId());
        entity.setImpressionAt(domain.getImpressionAt());
        entity.setViewDurationMs(domain.getViewDurationMs());
        entity.setClickedAt(domain.getClickedAt());
        entity.setConvertedAt(domain.getConvertedAt());
        entity.setConversionType(domain.getConversionType());
        entity.setConversionValue(domain.getConversionValue());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
