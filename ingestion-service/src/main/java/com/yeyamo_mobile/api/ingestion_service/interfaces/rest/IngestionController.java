package com.yeyamo_mobile.api.ingestion_service.interfaces.rest;
import java.util.UUID;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.ingestion_service.application.IngestionJobService;import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;import org.springframework.validation.annotation.Validated;
@RestController @RequestMapping("/api/v1/catalog/imports") @Validated
public class IngestionController{
 private final IngestionJobService service;public IngestionController(IngestionJobService s){service=s;}
 @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
 public ImportJobResponse submit(@RequestHeader("Idempotency-Key")@NotBlank String key,@Valid @RequestBody ImportRequest request){
  return ImportJobResponse.from(service.submit(key,request.sourceType(),request.sourceReference(),request.payload()));}
 @GetMapping("/{id}")public ImportJobResponse get(@PathVariable UUID id){return ImportJobResponse.from(service.get(id));}
}
