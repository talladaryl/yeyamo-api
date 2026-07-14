package com.yeyamo_mobile.api.partner_service.domain.port;
import java.util.List;import java.util.Optional;import java.util.UUID;import com.yeyamo_mobile.api.partner_service.domain.model.PartnerDocument;
public interface PartnerDocumentRepository { PartnerDocument save(PartnerDocument d); List<PartnerDocument> findByPartnerId(UUID id); Optional<PartnerDocument> findDocumentById(UUID id); void delete(PartnerDocument d); }
