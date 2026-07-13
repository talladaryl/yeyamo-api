package com.yeyamo_mobile.api.partner_service.interfaces.rest;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.partner_service.domain.model.*;
public record DocumentResponse(UUID id,DocumentType type,String filename,String contentType,long sizeBytes,Instant uploadedAt){public static DocumentResponse from(PartnerDocument d){return new DocumentResponse(d.id(),d.documentType(),d.originalFilename(),d.contentType(),d.sizeBytes(),d.uploadedAt());}}
