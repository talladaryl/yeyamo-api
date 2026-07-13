package com.yeyamo_mobile.api.media_service.interfaces.rest;
import java.io.IOException;import java.util.*;import org.springframework.core.io.InputStreamResource;import org.springframework.http.*;import org.springframework.security.core.*;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
import com.yeyamo_mobile.api.media_service.application.MediaApplicationService;import com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort;
import io.swagger.v3.oas.annotations.*;import io.swagger.v3.oas.annotations.media.*;import io.swagger.v3.oas.annotations.responses.*;import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
@RestController @RequestMapping("/api/v1/media") @Tag(name="Media",description="Upload, metadata and delivery of YeYamo media")
public class MediaController{
 private final MediaApplicationService service;public MediaController(MediaApplicationService s){service=s;}
 @Operation(summary="Upload an image or video",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="201",description="Media stored")
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)@ResponseStatus(HttpStatus.CREATED)
 public MediaResponse upload(@RequestPart("file")MultipartFile file,@RequestParam(required=false)String altText,@RequestParam(required=false)String aggregateType,
   @RequestParam(required=false)String aggregateId,@RequestHeader(value="X-Correlation-Id",required=false)String correlationId,Authentication auth)throws IOException{
  return MediaResponse.from(service.upload(auth.getName(),file.getOriginalFilename(),file.getContentType(),file.getBytes(),altText,aggregateType,aggregateId,correlationId));}
 @Operation(summary="Get public media metadata")@GetMapping("/{id}")public MediaResponse metadata(@PathVariable UUID id){return MediaResponse.from(service.metadata(id));}
 @Operation(summary="Download original content")@GetMapping("/{id}/content")public ResponseEntity<InputStreamResource> content(@PathVariable UUID id){return stream(service.content(id,false));}
 @Operation(summary="Download generated thumbnail")@GetMapping("/{id}/thumbnail")public ResponseEntity<InputStreamResource> thumbnail(@PathVariable UUID id){return stream(service.content(id,true));}
 @Operation(summary="Delete owned media",security=@SecurityRequirement(name="bearerAuth"))@DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlationId,Authentication auth){
  boolean admin=auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a->a.equals("ROLE_ADMIN")||a.equals("ROLE_SUPER_ADMIN"));service.delete(id,auth.getName(),admin,correlationId);}
 private ResponseEntity<InputStreamResource> stream(ObjectStoragePort.StoredObject o){return ResponseEntity.ok().contentType(MediaType.parseMediaType(o.contentType())).contentLength(o.length())
  .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic()).body(new InputStreamResource(o.content()));}
}
