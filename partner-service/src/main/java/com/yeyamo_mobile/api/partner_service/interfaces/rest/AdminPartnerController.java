package com.yeyamo_mobile.api.partner_service.interfaces.rest;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.partner_service.application.AdminPartnerService;
import com.yeyamo_mobile.api.partner_service.domain.model.PartnerStatus;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/admin/partners")
public class AdminPartnerController {
    private final AdminPartnerService service;
    public AdminPartnerController(AdminPartnerService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAnyRole('SUPPORT','ADMIN','SUPER_ADMIN')")
    public Page<AdminPartnerResponse.Summary> list(@RequestParam(required=false)String search,@RequestParam(required=false)PartnerStatus status,@RequestParam(required=false)Instant createdFrom,@RequestParam(required=false)Instant createdTo,Pageable pageable){return service.search(search,status,createdFrom,createdTo,pageable);}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('SUPPORT','ADMIN','SUPER_ADMIN')")
    public AdminPartnerResponse.Detail detail(@PathVariable UUID id){return service.detail(id);}
    @GetMapping("/{id}/kyc") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<AdminPartnerResponse.Document> kyc(@PathVariable UUID id){return service.kyc(id);}
    @GetMapping("/{id}/validation-history") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<AdminPartnerResponse.History> history(@PathVariable UUID id){return service.validationHistory(id);}
    @GetMapping("/{id}/establishments") @PreAuthorize("hasAnyRole('SUPPORT','ADMIN','SUPER_ADMIN')")
    public JsonNode establishments(@PathVariable UUID id,@RequestHeader(HttpHeaders.AUTHORIZATION)String authorization){return service.establishments(id,authorization);}
    @GetMapping("/{partnerId}/kyc/documents/{documentId}") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<InputStreamResource> document(@PathVariable UUID partnerId,@PathVariable UUID documentId)throws IOException{var file=service.open(partnerId,documentId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).contentLength(file.size()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.filename()).build().toString()).header(HttpHeaders.CACHE_CONTROL,"no-store").body(new InputStreamResource(file.content()));}
}
