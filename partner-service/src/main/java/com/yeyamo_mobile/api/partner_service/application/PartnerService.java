package com.yeyamo_mobile.api.partner_service.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.partner_service.application.port.*;
import com.yeyamo_mobile.api.partner_service.domain.model.*;
import com.yeyamo_mobile.api.partner_service.domain.port.*;

@Service
public class PartnerService {
    private static final Set<String> TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private final PartnerRepository partners;
    private final PartnerDocumentRepository documents;
    private final DocumentStoragePort storage;
    private final PartnerOutboxPort outbox;
    private final KycPolicy kycPolicy;
    private final long maxSize;

    public PartnerService(PartnerRepository p, PartnerDocumentRepository d, DocumentStoragePort s,
            PartnerOutboxPort o, KycPolicy policy,
            @org.springframework.beans.factory.annotation.Value("${yeyamo.documents.max-size-bytes:10485760}") long max) {
        partners = p; documents = d; storage = s; outbox = o; kycPolicy = policy; maxSize = max;
    }

    @Transactional
    public Partner create(String owner, String legal, String trade, BusinessType type, String registration,
            String tax, String email, String phone, String website, String description, String correlation) {
        return create(owner, legal, trade, type, registration, tax, email, phone, website, description,
                null, null, correlation);
    }

    @Transactional
    public Partner create(String owner, String legal, String trade, BusinessType type, String registration,
            String tax, String email, String phone, String website, String description, String primaryCountry,
            Set<String> operatingCountries, String correlation) {
        if (partners.existsByOwnerUserId(owner))
            throw error("PARTNER_ALREADY_EXISTS", "Un compte partenaire existe deja", HttpStatus.CONFLICT);
        Partner partner = Partner.create(owner, legal, type, email);
        partner.update(legal, trade, type, registration, tax, email, phone, website, description);
        partner.setPrimaryCountryCode(primaryCountry);
        partner.setOperatingCountries(operatingCountries);
        partner = partners.save(partner);
        event("partner.created", partner, owner, correlation);
        return partner;
    }

    @Transactional(readOnly = true) public Partner me(String owner) { return owned(owner); }
    @Transactional(readOnly = true) public List<PartnerDocument> myDocuments(String owner) { return documents.findByPartnerId(owned(owner).getId()); }
    @Transactional(readOnly = true)
    public Partner publicPartner(UUID id) {
        Partner partner = partners.findById(id).orElseThrow(this::notFound);
        if (!partner.isPublic()) throw error("PARTNER_NOT_ACCESSIBLE", "Partenaire non accessible", HttpStatus.NOT_FOUND);
        return partner;
    }
    @Transactional(readOnly = true) public Page<Partner> search(String q, Pageable pageable) { return partners.searchApproved(q, pageable); }

    @Transactional
    public Partner update(String owner, String legal, String trade, BusinessType type, String registration,
            String tax, String email, String phone, String website, String description, String correlation) {
        return update(owner, legal, trade, type, registration, tax, email, phone, website, description,
                null, null, correlation);
    }

    @Transactional
    public Partner update(String owner, String legal, String trade, BusinessType type, String registration,
            String tax, String email, String phone, String website, String description, String primaryCountry,
            Set<String> operatingCountries, String correlation) {
        Partner partner = owned(owner);
        try {
            partner.update(legal, trade, type, registration, tax, email, phone, website, description);
            if (primaryCountry != null) partner.setPrimaryCountryCode(primaryCountry);
            if (operatingCountries != null) partner.setOperatingCountries(operatingCountries);
        } catch (IllegalStateException exception) { throw state(exception); }
        Partner saved = partners.save(partner);
        event("partner.updated", saved, owner, correlation);
        return saved;
    }

    @Transactional
    public PartnerDocument addDocument(String owner, DocumentType type, String filename, String contentType,
            long size, InputStream stream, String correlation) throws IOException {
        Partner partner = owned(owner);
        if (!partner.canEditDocuments()) throw error("PARTNER_NOT_EDITABLE", "Les documents ne peuvent plus etre modifies", HttpStatus.CONFLICT);
        if (size <= 0 || size > maxSize) throw error("DOCUMENT_SIZE_INVALID", "Taille de document invalide", HttpStatus.BAD_REQUEST);
        if (!TYPES.contains(contentType)) throw error("DOCUMENT_TYPE_INVALID", "Format autorise: PDF, JPEG ou PNG", HttpStatus.BAD_REQUEST);
        String safeName = filename == null || filename.isBlank() ? "document" : filename;
        String key = storage.store(partner.getId().toString(), safeName, contentType, stream);
        PartnerDocument document = documents.save(PartnerDocument.create(partner.getId(), type, key, safeName, contentType, size));
        event("partner.document_added", partner, owner, correlation);
        return document;
    }

    @Transactional
    public void removeDocument(String owner, UUID documentId, String correlation) throws IOException {
        Partner partner = owned(owner);
        if (!partner.canEditDocuments()) throw error("PARTNER_NOT_EDITABLE", "Les documents ne peuvent plus etre modifies", HttpStatus.CONFLICT);
        PartnerDocument document = documents.findDocumentById(documentId)
                .orElseThrow(() -> error("DOCUMENT_NOT_FOUND", "Document introuvable", HttpStatus.NOT_FOUND));
        if (!document.partnerId().equals(partner.getId())) throw error("DOCUMENT_NOT_FOUND", "Document introuvable", HttpStatus.NOT_FOUND);
        documents.delete(document); storage.delete(document.storageKey()); event("partner.document_removed", partner, owner, correlation);
    }

    @Transactional
    public Partner submit(String owner, String correlation) {
        Partner partner = owned(owner);
        List<PartnerDocument> docs = documents.findByPartnerId(partner.getId());
        kycPolicy.validate(partner, docs);
        try { partner.submit(docs.size()); } catch (IllegalStateException exception) { throw state(exception); }
        Partner saved = partners.save(partner);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerId", partner.getId()); payload.put("requesterId", owner);
        payload.put("countryCode", partner.getPrimaryCountryCode());
        payload.put("kycDocumentKeys", docs.stream().map(PartnerDocument::storageKey).toList());
        payload.put("kycDocumentTypes", docs.stream().map(d -> d.documentType().name()).toList());
        outbox.append("partner.submitted", partner.getId(), owner, correlation, payload);
        return saved;
    }

    @Transactional
    public Partner applyDecision(UUID id, PartnerStatus decision, String comment, Integer risk, String correlation) {
        Partner partner = partners.findById(id).orElseThrow(this::notFound);
        try { partner.review(decision, comment, risk); } catch (IllegalStateException | IllegalArgumentException exception) { throw state(exception); }
        Partner saved = partners.save(partner);
        event("partner." + decision.name().toLowerCase(), saved, "admin-service", correlation);
        return saved;
    }

    private Partner owned(String owner) { return partners.findByOwnerUserId(owner).orElseThrow(this::notFound); }
    private PartnerException notFound() { return error("PARTNER_NOT_FOUND", "Partenaire introuvable", HttpStatus.NOT_FOUND); }
    private PartnerException state(Exception exception) { return error("INVALID_PARTNER_STATE", exception.getMessage(), HttpStatus.CONFLICT); }
    private PartnerException error(String code, String message, HttpStatus status) { return new PartnerException(code, message, status); }

    private void event(String type, Partner partner, String actor, String correlation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerId", partner.getId()); payload.put("ownerUserId", partner.getOwnerUserId());
        payload.put("status", partner.getStatus().name()); payload.put("legalName", partner.getLegalName());
        payload.put("countryCode", partner.getPrimaryCountryCode());
        outbox.append(type, partner.getId(), actor, correlation, payload);
    }
}
