package com.yeyamo_mobile.api.media_service.interfaces.rest;
import java.io.IOException;import java.util.*;import org.springframework.core.io.InputStreamResource;import org.springframework.http.*;import org.springframework.security.core.*;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
import com.yeyamo_mobile.api.media_service.application.MediaApplicationService;import com.yeyamo_mobile.api.media_service.application.MediaContentPolicy;
import com.yeyamo_mobile.api.media_service.application.SignedUrlService;
import com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort;
import com.yeyamo_mobile.api.media_service.domain.model.ConsentStatus;
import com.yeyamo_mobile.api.media_service.domain.model.MediaUsageType;
import io.swagger.v3.oas.annotations.*;import io.swagger.v3.oas.annotations.media.*;import io.swagger.v3.oas.annotations.responses.*;import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController @RequestMapping("/api/v1/media") @Tag(name="Media",description="Upload, metadata and delivery of YeYamo media")
public class MediaController{
 private final MediaApplicationService service;
 private final MediaContentPolicy policy;
 private final SignedUrlService signedUrlService;

 public MediaController(MediaApplicationService s, MediaContentPolicy p, SignedUrlService su){
  service=s; policy=p; signedUrlService=su;
 }

 // ---- Upload (original endpoint — no usageType, fully backwards-compatible) ----
 @Operation(summary="Upload an image or video",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="201",description="Media stored")
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)@ResponseStatus(HttpStatus.CREATED)
 public MediaResponse upload(
   @RequestPart("file") MultipartFile file,
   @RequestParam(required=false) String altText,
   @RequestParam(required=false) String aggregateType,
   @RequestParam(required=false) String aggregateId,
   @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,
   Authentication auth) throws IOException{
  policy.validateDeclaredSize(file.getContentType(),file.getSize());
  return MediaResponse.from(service.upload(auth.getName(),file.getOriginalFilename(),
   file.getContentType(),file.getBytes(),altText,aggregateType,aggregateId,correlationId));}

 // ---- Upload (extended endpoint — with usageType and rights metadata) ----------
 @Operation(summary="Upload culture or artisan media with usage type and rights",
            security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="201",description="Media stored")
 @PostMapping(path="/culture",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 @ResponseStatus(HttpStatus.CREATED)
 public MediaResponse uploadCulture(
   @RequestPart("file") MultipartFile file,
   @RequestParam MediaUsageType usageType,
   @RequestParam(required=false) String altText,
   @RequestParam(required=false) String aggregateType,
   @RequestParam(required=false) String aggregateId,
   // Rights
   @RequestParam(required=false) String copyrightOwner,
   @RequestParam(required=false) String licenseType,
   @RequestParam(required=false) String usagePermission,
   @RequestParam(required=false) Boolean attributionRequired,
   // Consent
   @RequestParam(required=false) ConsentStatus consentStatus,
   @RequestParam(required=false) String consentRecordId,
   @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,
   Authentication auth) throws IOException{
  policy.validateDeclaredSize(file.getContentType(),file.getSize());
  boolean isArtisan=auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
   .anyMatch(a->a.equals("ROLE_ARTISAN")||a.equals("ROLE_PARTNER")||a.equals("ROLE_ADMIN"));
  return MediaResponse.from(service.upload(
   auth.getName(),file.getOriginalFilename(),file.getContentType(),file.getBytes(),
   altText,aggregateType,aggregateId,correlationId,
   usageType,isArtisan,copyrightOwner,licenseType,usagePermission,
   attributionRequired,consentStatus,consentRecordId));}

 // ---- Metadata ----------------------------------------------------------------
 @Operation(summary="Get public media metadata")
 @GetMapping("/{id}")
 public MediaResponse metadata(@PathVariable UUID id){
  return MediaResponse.from(service.metadata(id));}

 // ---- Content delivery --------------------------------------------------------
 @Operation(summary="Download original content (public for IMAGE/VIDEO/AUDIO)")
 @GetMapping("/{id}/content")
 public ResponseEntity<InputStreamResource> content(
   @PathVariable UUID id,
   @RequestParam(required=false) Long expires,
   @RequestParam(required=false) String sig){
  // DOCUMENT and CERTIFICATE require a valid signed URL
  var meta = service.metadata(id);
  if(meta.getType()==com.yeyamo_mobile.api.media_service.domain.model.MediaType.DOCUMENT
   ||meta.getType()==com.yeyamo_mobile.api.media_service.domain.model.MediaType.CERTIFICATE){
   if(expires==null||sig==null) throw new com.yeyamo_mobile.api.media_service.application.MediaException("SIGNED_URL_REQUIRED","A signed URL is required for this media type");
   return stream(service.protectedContent(id,expires,sig,signedUrlService));
  }
  return stream(service.content(id,false));}

 @Operation(summary="Download generated thumbnail")
 @GetMapping("/{id}/thumbnail")
 public ResponseEntity<InputStreamResource> thumbnail(@PathVariable UUID id){
  return stream(service.content(id,true));}

 @Operation(summary="Generate a signed URL for protected media (DOCUMENT/CERTIFICATE)",
            security=@SecurityRequirement(name="bearerAuth"))
 @PostMapping("/{id}/signed-url")
 public Map<String,String> signedUrl(
   @PathVariable UUID id,
   @RequestParam(required=false) Long ttlMinutes,
   Authentication auth){
  var meta = service.metadata(id);
  // Only owner or admin may request a signed URL
  boolean admin=auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
   .anyMatch(a->a.equals("ROLE_ADMIN")||a.equals("ROLE_SUPER_ADMIN"));
  if(!admin&&!meta.getOwnerId().equals(auth.getName()))
   throw new com.yeyamo_mobile.api.media_service.application.MediaException("MEDIA_FORBIDDEN","Only the owner can create a signed URL");
  String url = signedUrlService.generate(id,"/api/v1/media",ttlMinutes);
  return Map.of("signedUrl",url);}

 // ---- Delete ------------------------------------------------------------------
 @Operation(summary="Delete owned media",security=@SecurityRequirement(name="bearerAuth"))
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(
   @PathVariable UUID id,
   @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,
   Authentication auth){
  boolean admin=auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
   .anyMatch(a->a.equals("ROLE_ADMIN")||a.equals("ROLE_SUPER_ADMIN"));
  service.delete(id,auth.getName(),admin,correlationId);}

 // ---- Helpers -----------------------------------------------------------------
 private ResponseEntity<InputStreamResource> stream(ObjectStoragePort.StoredObject o){
  return ResponseEntity.ok()
   .contentType(MediaType.parseMediaType(o.contentType()))
   .contentLength(o.length())
   .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
   .body(new InputStreamResource(o.content()));}
}
