package com.yeyamo_mobile.api.partner_service.infrastructure.persistence;
import java.util.List;import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;
public interface SpringPartnerDocumentRepository extends JpaRepository<PartnerDocumentEntity,UUID>{List<PartnerDocumentEntity>findByPartnerIdOrderByUploadedAtDesc(UUID id);}
