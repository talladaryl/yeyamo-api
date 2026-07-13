package com.yeyamo_mobile.api.ingestion_service.infrastructure.persistence;
import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;import com.yeyamo_mobile.api.ingestion_service.domain.model.RecordStatus;
public interface SpringIngestionRecordRepository extends JpaRepository<IngestionRecordEntity,UUID>{boolean existsByFingerprintAndStatus(String fingerprint,RecordStatus status);}
