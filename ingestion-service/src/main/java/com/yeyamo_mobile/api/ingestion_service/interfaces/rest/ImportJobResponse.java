package com.yeyamo_mobile.api.ingestion_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.ingestion_service.domain.model.*;
public record ImportJobResponse(UUID id,String idempotencyKey,SourceType sourceType,String sourceReference,JobStatus status,
 int totalRecords,int acceptedRecords,int rejectedRecords,int duplicateRecords,String errorMessage,Instant createdAt,Instant startedAt,Instant completedAt){
 public static ImportJobResponse from(ImportJob j){return new ImportJobResponse(j.getId(),j.getIdempotencyKey(),j.getSourceType(),j.getSourceReference(),j.getStatus(),
  j.getTotalRecords(),j.getAcceptedRecords(),j.getRejectedRecords(),j.getDuplicateRecords(),j.getErrorMessage(),j.getCreatedAt(),j.getStartedAt(),j.getCompletedAt());}
}
