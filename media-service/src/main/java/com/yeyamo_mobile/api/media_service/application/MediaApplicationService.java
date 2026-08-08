package com.yeyamo_mobile.api.media_service.application;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.time.*;import java.util.*;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.media_service.application.port.*;import com.yeyamo_mobile.api.media_service.application.thumbnail.ThumbnailStrategy;
import com.yeyamo_mobile.api.media_service.domain.model.*;import com.yeyamo_mobile.api.media_service.domain.port.MediaRepository;

@Service
public class MediaApplicationService{
 private static final Logger log = LoggerFactory.getLogger(MediaApplicationService.class);
 private final MediaRepository repository;private final ObjectStoragePort storage;private final MediaOutboxPort outbox;
 private final MediaContentPolicy policy;private final List<ThumbnailStrategy> thumbnails;
 private final MediaQuotaService quota;private final AudioTranscodingService transcoding;

 @org.springframework.beans.factory.annotation.Autowired
 public MediaApplicationService(
   MediaRepository r,ObjectStoragePort s,MediaOutboxPort o,MediaContentPolicy p,List<ThumbnailStrategy> t,
   MediaQuotaService q,AudioTranscodingService a){
  repository=r;storage=s;outbox=o;policy=p;thumbnails=t;quota=q;transcoding=a;}

 /** Backwards-compatible constructor used by existing tests (5-arg form). */
 public MediaApplicationService(
   MediaRepository r,ObjectStoragePort s,MediaOutboxPort o,MediaContentPolicy p,List<ThumbnailStrategy> t){
  this(r,s,o,p,t,
   new MediaQuotaService(new com.yeyamo_mobile.api.media_service.infrastructure.persistence.InMemoryMediaQuotaRepository(),
    52428800L,524288000L,26214400L,26214400L,104857600L,52428800L,262144000L,1073741824L),
   new AudioTranscodingService(false,"ffmpeg",60L));}

 // ---- Upload (original path — no usageType) --------------------------------
 @Transactional
 public MediaAsset upload(String ownerId,String filename,String contentType,byte[] bytes,
   String altText,String aggregateType,String aggregateId,String correlationId){
  return upload(ownerId,filename,contentType,bytes,altText,aggregateType,aggregateId,
   correlationId,null,false,null,null,null,null,null,null);}

 // ---- Upload (extended path — with usageType and rights metadata) ----------
 @Transactional
 public MediaAsset upload(
   String ownerId,String filename,String contentType,byte[] bytes,
   String altText,String aggregateType,String aggregateId,String correlationId,
   MediaUsageType usageType,boolean isArtisan,
   String copyrightOwner,String licenseType,String usagePermission,
   Boolean attributionRequired,ConsentStatus consentStatus,String consentRecordId){

  // 1. Validate MIME + magic bytes
  MediaType type = policy.validate(contentType, bytes);

  // 2. Resolve effective type (PDF → DOCUMENT or CERTIFICATE based on usageType)
  MediaType effectiveType = policy.resolveEffectiveType(type, usageType);

  // 3. Document/Certificate MIME whitelist
  if(effectiveType==MediaType.DOCUMENT||effectiveType==MediaType.CERTIFICATE){
   policy.validateDocumentMime(contentType);}

  // 4. Quota check (before any storage)
  quota.checkAndEnforce(ownerId, isArtisan, effectiveType, usageType, bytes.length);

  // 5. Dedup check
  String checksum = sha256(bytes);
  if(repository.existsByChecksumAndOwnerId(checksum,ownerId))
   throw new MediaException("MEDIA_DUPLICATE","This media was already uploaded");

  // 6. Audio transcoding (best-effort, non-blocking failure)
  byte[] finalBytes = bytes;
  String finalContentType = contentType;
  if(effectiveType==MediaType.AUDIO && transcoding.isEnabled()){
   AudioTranscodingService.TranscodeResult result = transcoding.transcode(bytes,contentType);
   finalBytes = result.bytes();
   finalContentType = result.contentType();
   if(result.transcoded()) checksum = sha256(finalBytes);}

  // 7. Store original (or transcoded) file
  String safeFilename = safe(filename);
  String originalKey = key("originals", safeFilename);
  String stored = null, thumbKey = null;

  try{
   stored = storage.store(originalKey,new ByteArrayInputStream(finalBytes),finalBytes.length,finalContentType);

   // 8. Create domain asset with effective type
   MediaAsset media;
   if(usageType!=null||copyrightOwner!=null||consentStatus!=null){
    media = MediaAsset.createWithUsage(ownerId,effectiveType,usageType,safeFilename,finalContentType,
     finalBytes.length,checksum,stored,altText,aggregateType,aggregateId,
     copyrightOwner,licenseType,usagePermission,attributionRequired,consentStatus,consentRecordId);
   } else {
    media = MediaAsset.create(ownerId,effectiveType,safeFilename,finalContentType,finalBytes.length,checksum,stored,altText,aggregateType,aggregateId);
   }

   // 9. Set audio duration placeholder (duration extraction requires ffprobe; deferred to async pipeline)
   media = repository.save(media);
   outbox.append("media.uploaded",media,correlationId,ownerId);

   // 10. Thumbnail
   try{
    ThumbnailStrategy strategy = thumbnails.stream().filter(t->t.supports(effectiveType)).findFirst().orElseThrow();
    ThumbnailStrategy.Thumbnail thumb = strategy.generate(finalBytes,finalContentType);
    thumbKey = key("thumbnails",media.getId()+".jpg");
    storage.store(thumbKey,new ByteArrayInputStream(thumb.bytes()),thumb.bytes().length,thumb.contentType());
    media.ready(thumbKey,thumb.width(),thumb.height(),thumb.durationMs());
   }catch(Exception thumbnailFailure){
    safeDelete(thumbKey); thumbKey=null;
    media.readyWithoutThumbnail(thumbnailFailure.getMessage(),null);
    log.warn("Thumbnail generation failed for media {}: {}",media.getId(),thumbnailFailure.getMessage());
   }

   media = repository.save(media);
   String eventType = media.getThumbnailStatus()==ThumbnailStatus.READY?"media.ready":"media.thumbnail_failed";
   outbox.append(eventType,media,correlationId,ownerId);

   // 11. Record quota after successful upload
   quota.recordUpload(ownerId,effectiveType,finalBytes.length);

   return media;

  }catch(RuntimeException failure){
   if(thumbKey!=null)safeDelete(thumbKey);
   if(stored!=null)safeDelete(stored);
   outbox.append("media.rejected",
    MediaAsset.create(ownerId,type,safeFilename,contentType,bytes.length,checksum,"rejected",altText,aggregateType,aggregateId),
    correlationId,ownerId);
   throw failure;
  }
 }

 @Transactional(readOnly=true)
 public MediaAsset metadata(UUID id){
  MediaAsset m=required(id);
  if(!m.readable())throw new MediaException("MEDIA_NOT_AVAILABLE","Media is not available");
  return m;}

 @Transactional(readOnly=true)
 public ObjectStoragePort.StoredObject content(UUID id,boolean thumbnail){
  MediaAsset m=metadata(id);
  String k=thumbnail?m.getThumbnailKey():m.getStorageKey();
  if(k==null)throw new MediaException("THUMBNAIL_NOT_AVAILABLE","Thumbnail is not available");
  ObjectStoragePort.StoredObject stored=storage.open(k);
  return new ObjectStoragePort.StoredObject(stored.content(),stored.length(),thumbnail?"image/jpeg":m.getContentType());}

 /**
  * Retrieve protected content only when the signed URL token is valid.
  * Used by DOCUMENT and CERTIFICATE assets.
  */
 @Transactional(readOnly=true)
 public ObjectStoragePort.StoredObject protectedContent(UUID id, long expiresEpoch, String sig,
   SignedUrlService signedUrlService){
  signedUrlService.validate(id,expiresEpoch,sig);
  return content(id,false);}

 @Transactional
 public void delete(UUID id,String actorId,boolean admin,String correlationId){
  MediaAsset m=required(id);
  if(!admin&&!m.getOwnerId().equals(actorId))
   throw new MediaException("MEDIA_FORBIDDEN","Only the owner can delete this media");
  m.delete();repository.save(m);outbox.append("media.deleted",m,correlationId,actorId);
  safeDelete(m.getStorageKey());safeDelete(m.getThumbnailKey());}

 // -------------------------------------------------------------------------
 private MediaAsset required(UUID id){return repository.findById(id).orElseThrow(()->new MediaException("MEDIA_NOT_FOUND","Media not found"));}
 private String key(String prefix,String filename){LocalDate d=LocalDate.now(ZoneOffset.UTC);return prefix+"/"+d.getYear()+"/"+String.format("%02d",d.getMonthValue())+"/"+UUID.randomUUID()+"-"+filename;}
 private String safe(String filename){String f=filename==null?"upload":filename.replace("\\","/");f=f.substring(f.lastIndexOf('/')+1).replaceAll("[^a-zA-Z0-9._-]","_");return f.isBlank()?"upload":f.substring(0,Math.min(f.length(),180));}
 private String sha256(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private void safeDelete(String key){if(key==null)return;try{storage.delete(key);}catch(RuntimeException ignored){}}
}
