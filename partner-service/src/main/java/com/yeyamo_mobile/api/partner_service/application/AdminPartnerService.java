package com.yeyamo_mobile.api.partner_service.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.yeyamo_mobile.api.partner_service.application.port.DocumentStoragePort;
import com.yeyamo_mobile.api.partner_service.domain.model.PartnerStatus;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.*;
import com.yeyamo_mobile.api.partner_service.interfaces.rest.AdminPartnerResponse;

@Service
public class AdminPartnerService {
    private final SpringPartnerRepository partners; private final SpringPartnerDocumentRepository documents; private final PartnerValidationHistoryRepository history; private final DocumentStoragePort storage; private final RestClient placeClient;
    public AdminPartnerService(SpringPartnerRepository partners,SpringPartnerDocumentRepository documents,PartnerValidationHistoryRepository history,DocumentStoragePort storage,@Value("${yeyamo.services.place-service.url:http://place-service:8080}")String placeServiceUrl){this.partners=partners;this.documents=documents;this.history=history;this.storage=storage;this.placeClient=RestClient.builder().baseUrl(placeServiceUrl).build();}
    @Transactional(readOnly=true) public Page<AdminPartnerResponse.Summary> search(String search,PartnerStatus status,Instant from,Instant to,Pageable pageable){return partners.adminSearch(search==null?"":search.trim(),status,from,to,pageable).map(AdminPartnerResponse::summary);}
    @Transactional(readOnly=true) public AdminPartnerResponse.Detail detail(UUID id){PartnerEntity partner=require(id);return new AdminPartnerResponse.Detail(AdminPartnerResponse.summary(partner),kyc(id),validationHistory(id));}
    @Transactional(readOnly=true) public List<AdminPartnerResponse.Document> kyc(UUID id){require(id);return documents.findByPartnerIdOrderByUploadedAtDesc(id).stream().map(AdminPartnerResponse::document).toList();}
    @Transactional(readOnly=true) public List<AdminPartnerResponse.History> validationHistory(UUID id){require(id);return history.findByPartnerIdOrderByCreatedAtDesc(id).stream().map(AdminPartnerResponse::history).toList();}
    @Transactional(readOnly=true) public StoredDocument open(UUID partnerId,UUID documentId)throws IOException{require(partnerId);PartnerDocumentEntity document=documents.findByIdAndPartnerId(documentId,partnerId).orElseThrow(()->new PartnerException("DOCUMENT_NOT_FOUND","Document introuvable",HttpStatus.NOT_FOUND));return new StoredDocument(document.getOriginalFilename(),document.getContentType(),document.getSizeBytes(),storage.open(document.getStorageKey()));}
    @Transactional(readOnly=true) public JsonNode establishments(UUID partnerId,String authorization){require(partnerId);return placeClient.get().uri(uri->uri.path("/api/v1/admin/places").queryParam("partnerId",partnerId).queryParam("size",100).build()).header("Authorization",authorization).retrieve().body(JsonNode.class);}
    private PartnerEntity require(UUID id){return partners.findById(id).orElseThrow(()->new PartnerException("PARTNER_NOT_FOUND","Partenaire introuvable",HttpStatus.NOT_FOUND));}
    public record StoredDocument(String filename,String contentType,long size,InputStream content){}
}
