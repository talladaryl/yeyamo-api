package com.yeyamo_mobile.api.ingestion_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.JpaRepository;
import com.yeyamo_mobile.api.ingestion_service.domain.model.JobStatus;
public interface SpringImportJobRepository extends JpaRepository<ImportJobEntity,UUID>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<ImportJobEntity>{
 Optional<ImportJobEntity> findByIdempotencyKey(String key);
 List<ImportJobEntity> findByStatusOrderByCreatedAtAsc(JobStatus status,Pageable pageable);
}
