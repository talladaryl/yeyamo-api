package com.yeyamo_mobile.api.media_service.application;
import java.io.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.time.*;import java.util.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.media_service.application.port.*;import com.yeyamo_mobile.api.media_service.application.thumbnail.ThumbnailStrategy;
import com.yeyamo_mobile.api.media_service.domain.model.*;import com.yeyamo_mobile.api.media_service.domain.port.MediaRepository;
@Service
public class MediaApplicationService{
 private final MediaRepository repository;private final ObjectStoragePort storage;private final MediaOutboxPort outbox;private final MediaContentPolicy policy;private final List<ThumbnailStrategy> thumbnails;
 public MediaApplicationService(MediaRepository r,ObjectStoragePort s,MediaOutboxPort o,MediaContentPolicy p,List<ThumbnailStrategy> t){repository=r;storage=s;outbox=o;policy=p;thumbnails=t;}
 @Transactional public MediaAsset upload(String ownerId,String filename,String contentType,byte[] bytes,String altText,String aggregateType,String aggregateId,String correlationId){
  MediaType type=policy.validate(contentType,bytes);String checksum=sha256(bytes);if(repository.existsByChecksumAndOwnerId(checksum,ownerId))throw new MediaException("MEDIA_DUPLICATE","This media was already uploaded");
  String safeFilename=safe(filename);String originalKey=key("originals",safeFilename);String stored=null,thumbKey=null;
  try{stored=storage.store(originalKey,new ByteArrayInputStream(bytes),bytes.length,contentType);
   MediaAsset media=MediaAsset.create(ownerId,type,safeFilename,contentType,bytes.length,checksum,stored,altText,aggregateType,aggregateId);
   media=repository.save(media);outbox.append("media.uploaded",media,correlationId,ownerId);
   try{ThumbnailStrategy strategy=thumbnails.stream().filter(t->t.supports(type)).findFirst().orElseThrow();
    ThumbnailStrategy.Thumbnail thumb=strategy.generate(bytes,contentType);thumbKey=key("thumbnails",media.getId()+".jpg");
    storage.store(thumbKey,new ByteArrayInputStream(thumb.bytes()),thumb.bytes().length,thumb.contentType());
    media.ready(thumbKey,thumb.width(),thumb.height(),thumb.durationMs());
   }catch(Exception thumbnailFailure){safeDelete(thumbKey);thumbKey=null;media.readyWithoutThumbnail(thumbnailFailure.getMessage(),null);}
   media=repository.save(media);outbox.append(media.getThumbnailStatus()==ThumbnailStatus.READY?"media.ready":"media.thumbnail_failed",media,correlationId,ownerId);
   return media;
  }catch(RuntimeException failure){if(thumbKey!=null)safeDelete(thumbKey);if(stored!=null)safeDelete(stored);throw failure;}
 }
 @Transactional(readOnly=true)public MediaAsset metadata(UUID id){MediaAsset m=required(id);if(!m.readable())throw new MediaException("MEDIA_NOT_AVAILABLE","Media is not available");return m;}
 @Transactional(readOnly=true)public ObjectStoragePort.StoredObject content(UUID id,boolean thumbnail){MediaAsset m=metadata(id);String key=thumbnail?m.getThumbnailKey():m.getStorageKey();
  if(key==null)throw new MediaException("THUMBNAIL_NOT_AVAILABLE","Thumbnail is not available");ObjectStoragePort.StoredObject stored=storage.open(key);
  return new ObjectStoragePort.StoredObject(stored.content(),stored.length(),thumbnail?"image/jpeg":m.getContentType());}
 @Transactional public void delete(UUID id,String actorId,boolean admin,String correlationId){MediaAsset m=required(id);if(!admin&&!m.getOwnerId().equals(actorId))throw new MediaException("MEDIA_FORBIDDEN","Only the owner can delete this media");
  m.delete();repository.save(m);outbox.append("media.deleted",m,correlationId,actorId);safeDelete(m.getStorageKey());safeDelete(m.getThumbnailKey());}
 private MediaAsset required(UUID id){return repository.findById(id).orElseThrow(()->new MediaException("MEDIA_NOT_FOUND","Media not found"));}
 private String key(String prefix,String filename){LocalDate d=LocalDate.now(ZoneOffset.UTC);return prefix+"/"+d.getYear()+"/"+String.format("%02d",d.getMonthValue())+"/"+UUID.randomUUID()+"-"+filename;}
 private String safe(String filename){String f=filename==null?"upload":filename.replace("\\","/");f=f.substring(f.lastIndexOf('/')+1).replaceAll("[^a-zA-Z0-9._-]","_");return f.isBlank()?"upload":f.substring(0,Math.min(f.length(),180));}
 private String sha256(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private void safeDelete(String key){if(key==null)return;try{storage.delete(key);}catch(RuntimeException ignored){}}
}
