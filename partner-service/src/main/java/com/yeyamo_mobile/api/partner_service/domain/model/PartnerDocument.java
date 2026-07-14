package com.yeyamo_mobile.api.partner_service.domain.model;
import java.time.Instant;
import java.util.UUID;
public record PartnerDocument(UUID id, UUID partnerId, DocumentType documentType, String storageKey,
 String originalFilename, String contentType, long sizeBytes, Instant uploadedAt) {
 public static PartnerDocument create(UUID partnerId, DocumentType type, String key, String filename, String contentType, long size) {
  return new PartnerDocument(UUID.randomUUID(), partnerId, type, key, filename, contentType, size, Instant.now());
 }
}
