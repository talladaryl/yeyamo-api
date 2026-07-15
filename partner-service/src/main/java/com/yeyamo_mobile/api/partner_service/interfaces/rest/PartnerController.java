package com.yeyamo_mobile.api.partner_service.interfaces.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yeyamo_mobile.api.partner_service.application.PartnerService;
import com.yeyamo_mobile.api.partner_service.domain.model.DocumentType;
import com.yeyamo_mobile.api.partner_service.domain.model.Partner;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/partners")
public class PartnerController {
    private final PartnerService service;
    private final DocumentUploadPolicy uploadPolicy;

    public PartnerController(PartnerService service, DocumentUploadPolicy uploadPolicy) {
        this.service = service;
        this.uploadPolicy = uploadPolicy;
    }

    @PostMapping
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody PartnerRequest request,
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        Partner partner = service.create(authentication.getName(), request.legalName(), request.tradeName(),
                request.businessType(), request.registrationNumber(), request.taxId(), request.contactEmail(),
                request.contactPhone(), request.websiteUrl(), request.description(), correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PartnerResponse.from(partner));
    }

    @GetMapping("/me")
    public PartnerResponse me(Authentication authentication) {
        return PartnerResponse.from(service.me(authentication.getName()));
    }

    @PutMapping("/me")
    public PartnerResponse update(@Valid @RequestBody PartnerRequest request, Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return PartnerResponse.from(service.update(authentication.getName(), request.legalName(), request.tradeName(),
                request.businessType(), request.registrationNumber(), request.taxId(), request.contactEmail(),
                request.contactPhone(), request.websiteUrl(), request.description(), correlationId));
    }

    @GetMapping("/me/documents")
    public List<DocumentResponse> documents(Authentication authentication) {
        return service.myDocuments(authentication.getName()).stream().map(DocumentResponse::from).toList();
    }

    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@RequestParam DocumentType type,
            @RequestPart("file") MultipartFile file, Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) throws IOException {
        byte[] content = file.getBytes();
        uploadPolicy.validate(file.getContentType(), content);
        String filename = uploadPolicy.safeFilename(file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(service.addDocument(
                authentication.getName(), type, filename, file.getContentType(), content.length,
                new ByteArrayInputStream(content), correlationId)));
    }

    @DeleteMapping("/me/documents/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id, Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) throws IOException {
        service.removeDocument(authentication.getName(), id, correlationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/submit")
    public PartnerResponse submit(Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return PartnerResponse.from(service.submit(authentication.getName(), correlationId));
    }

    @GetMapping("/{id}")
    public PublicPartnerResponse byId(@PathVariable UUID id) {
        return PublicPartnerResponse.from(service.publicPartner(id));
    }

    @GetMapping
    public Page<PublicPartnerResponse> search(@RequestParam(defaultValue = "") String query, Pageable pageable) {
        return service.search(query, pageable).map(PublicPartnerResponse::from);
    }
}
