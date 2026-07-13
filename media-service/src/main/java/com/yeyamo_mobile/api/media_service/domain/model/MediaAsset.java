package com.yeyamo_mobile.api.media_service.domain.model;
import java.time.Instant;import java.util.UUID;
public class MediaAsset{
 private UUID id;private String ownerId;private MediaType type;private MediaStatus status;private ThumbnailStatus thumbnailStatus;
 private String originalFilename;private String contentType;private long sizeBytes;private String checksum;private String storageKey;private String thumbnailKey;
 private Integer width;private Integer height;private Long durationMs;private String altText;private String aggregateType;private String aggregateId;
 private String failureReason;private Instant createdAt;private Instant updatedAt;private Instant deletedAt;private long version;
 public static MediaAsset create(String ownerId,MediaType type,String filename,String contentType,long size,String checksum,String storageKey,
   String altText,String aggregateType,String aggregateId){if(ownerId==null||ownerId.isBlank())throw new IllegalArgumentException("ownerId is required");
  MediaAsset m=new MediaAsset();m.id=UUID.randomUUID();m.ownerId=ownerId;m.type=type;m.originalFilename=filename;m.contentType=contentType;m.sizeBytes=size;
  m.checksum=checksum;m.storageKey=storageKey;m.altText=trim(altText);m.aggregateType=trim(aggregateType);m.aggregateId=trim(aggregateId);
  m.status=MediaStatus.PROCESSING;m.thumbnailStatus=ThumbnailStatus.PENDING;m.createdAt=Instant.now();m.updatedAt=m.createdAt;return m;}
 public void ready(String thumb,Integer width,Integer height,Long duration){thumbnailKey=thumb;thumbnailStatus=ThumbnailStatus.READY;this.width=width;this.height=height;durationMs=duration;status=MediaStatus.READY;updatedAt=Instant.now();}
 public void readyWithoutThumbnail(String reason,Long duration){thumbnailStatus=ThumbnailStatus.FAILED;failureReason=abbreviate(reason);durationMs=duration;status=MediaStatus.READY;updatedAt=Instant.now();}
 public void fail(String reason){status=MediaStatus.FAILED;thumbnailStatus=ThumbnailStatus.FAILED;failureReason=abbreviate(reason);updatedAt=Instant.now();}
 public void delete(){if(status==MediaStatus.DELETED)return;status=MediaStatus.DELETED;deletedAt=Instant.now();updatedAt=deletedAt;}
 public boolean readable(){return status==MediaStatus.READY;}
 private static String trim(String v){return v==null||v.isBlank()?null:v.trim();}
 private String abbreviate(String v){if(v==null)return "Unknown media processing error";return v.substring(0,Math.min(v.length(),1000));}
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public String getOwnerId(){return ownerId;}public void setOwnerId(String v){ownerId=v;}
 public MediaType getType(){return type;}public void setType(MediaType v){type=v;}public MediaStatus getStatus(){return status;}public void setStatus(MediaStatus v){status=v;}
 public ThumbnailStatus getThumbnailStatus(){return thumbnailStatus;}public void setThumbnailStatus(ThumbnailStatus v){thumbnailStatus=v;}
 public String getOriginalFilename(){return originalFilename;}public void setOriginalFilename(String v){originalFilename=v;}public String getContentType(){return contentType;}public void setContentType(String v){contentType=v;}
 public long getSizeBytes(){return sizeBytes;}public void setSizeBytes(long v){sizeBytes=v;}public String getChecksum(){return checksum;}public void setChecksum(String v){checksum=v;}
 public String getStorageKey(){return storageKey;}public void setStorageKey(String v){storageKey=v;}public String getThumbnailKey(){return thumbnailKey;}public void setThumbnailKey(String v){thumbnailKey=v;}
 public Integer getWidth(){return width;}public void setWidth(Integer v){width=v;}public Integer getHeight(){return height;}public void setHeight(Integer v){height=v;}
 public Long getDurationMs(){return durationMs;}public void setDurationMs(Long v){durationMs=v;}public String getAltText(){return altText;}public void setAltText(String v){altText=v;}
 public String getAggregateType(){return aggregateType;}public void setAggregateType(String v){aggregateType=v;}public String getAggregateId(){return aggregateId;}public void setAggregateId(String v){aggregateId=v;}
 public String getFailureReason(){return failureReason;}public void setFailureReason(String v){failureReason=v;}public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}
 public Instant getUpdatedAt(){return updatedAt;}public void setUpdatedAt(Instant v){updatedAt=v;}public Instant getDeletedAt(){return deletedAt;}public void setDeletedAt(Instant v){deletedAt=v;}
 public long getVersion(){return version;}public void setVersion(long v){version=v;}
}
