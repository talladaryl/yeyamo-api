package com.yeyamo_mobile.api.ingestion_service.interfaces.rest;
import java.util.UUID;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.ingestion_service.application.IngestionJobService;import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.*;import java.time.Instant;import com.yeyamo_mobile.api.ingestion_service.domain.model.*;import org.springframework.security.access.prepost.PreAuthorize;
@RestController @RequestMapping("/api/v1/catalog/imports") @Validated
public class IngestionController{
 private final IngestionJobService service;public IngestionController(IngestionJobService s){service=s;}
 @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
 public ImportJobResponse submit(@RequestHeader("Idempotency-Key")@NotBlank String key,@Valid @RequestBody ImportRequest request){
  return ImportJobResponse.from(service.submit(key,request.sourceType(),request.sourceReference(),request.payload()));}
 @GetMapping("/{id}")public ImportJobResponse get(@PathVariable UUID id){return ImportJobResponse.from(service.get(id));}
 @GetMapping @PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')")public Page<ImportJobResponse>list(@RequestParam(required=false)JobStatus status,@RequestParam(required=false)SourceType source,@RequestParam(required=false)Instant createdFrom,@RequestParam(required=false)Instant createdTo,Pageable pageable){return service.list(status,source,createdFrom,createdTo,pageable).map(ImportJobResponse::from);}
 @PostMapping("/{id}/retry")@PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')")public ImportJobResponse retry(@PathVariable UUID id){return ImportJobResponse.from(service.retry(id));}
 @PostMapping("/{id}/cancel")@PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')")public ImportJobResponse cancel(@PathVariable UUID id){return ImportJobResponse.from(service.cancel(id));}
 @GetMapping("/{id}/errors")@PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')")public java.util.List<IngestionJobService.ImportError>errors(@PathVariable UUID id){return service.errors(id);}
}
