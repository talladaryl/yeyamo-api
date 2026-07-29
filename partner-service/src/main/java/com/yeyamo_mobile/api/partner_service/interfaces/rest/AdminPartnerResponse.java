package com.yeyamo_mobile.api.partner_service.interfaces.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.*;

public final class AdminPartnerResponse {
    private AdminPartnerResponse() {}
    public record Summary(UUID id,String ownerUserId,String legalName,String tradeName,String businessType,String contactEmail,String contactPhone,String status,Integer riskScore,Instant createdAt,Instant updatedAt){}
    public record Detail(Summary partner,List<Document> kycDocuments,List<History> validationHistory){}
    public record Document(UUID id,String type,String filename,String contentType,long sizeBytes,Instant uploadedAt,String downloadPath){}
    public record History(UUID id,String actorId,String decision,String reason,String comment,String correlationId,Instant timestamp){}
    public static Summary summary(PartnerEntity p){return new Summary(p.getId(),p.getOwnerUserId(),p.getLegalName(),p.getTradeName(),p.getBusinessType().name(),p.getContactEmail(),p.getContactPhone(),p.getStatus().name(),p.getRiskScore(),p.getCreatedAt(),p.getUpdatedAt());}
    public static Document document(PartnerDocumentEntity d){return new Document(d.getId(),d.getDocumentType().name(),d.getOriginalFilename(),d.getContentType(),d.getSizeBytes(),d.getUploadedAt(),"/api/v1/admin/partners/"+d.getPartnerId()+"/kyc/documents/"+d.getId());}
    public static History history(PartnerValidationHistoryEntity h){return new History(h.getId(),h.getActorId(),h.getDecision(),h.getReason(),h.getComment(),h.getCorrelationId(),h.getCreatedAt());}
}
