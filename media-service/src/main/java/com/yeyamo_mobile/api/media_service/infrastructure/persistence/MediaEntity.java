package com.yeyamo_mobile.api.media_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;import com.yeyamo_mobile.api.media_service.domain.model.*;
@Entity @Table(name="media_assets")
public class MediaEntity{
 @Id private UUID id;@Column(name="owner_id",nullable=false,length=100)private String ownerId;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private MediaType type;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private MediaStatus status;@Enumerated(EnumType.STRING)@Column(name="thumbnail_status",nullable=false,length=20)private ThumbnailStatus thumbnailStatus;
 @Column(name="original_filename",nullable=false,length=255)private String originalFilename;@Column(name="content_type",nullable=false,length=100)private String contentType;
 @Column(name="size_bytes",nullable=false)private long sizeBytes;@Column(nullable=false,length=64)private String checksum;@Column(name="storage_key",nullable=false,unique=true,length=500)private String storageKey;
 @Column(name="thumbnail_key",length=500)private String thumbnailKey;private Integer width;private Integer height;@Column(name="duration_ms")private Long durationMs;
 @Column(name="alt_text",length=500)private String altText;@Column(name="aggregate_type",length=80)private String aggregateType;@Column(name="aggregate_id",length=100)private String aggregateId;
 @Column(name="failure_reason",length=1000)private String failureReason;@Column(name="created_at",nullable=false)private Instant createdAt;@Column(name="updated_at",nullable=false)private Instant updatedAt;
 @Column(name="deleted_at")private Instant deletedAt;@Version private long version;
 // Culture & Artisan extensions
 @Enumerated(EnumType.STRING)@Column(name="usage_type",length=50)private MediaUsageType usageType;
 @Column(name="duration_seconds")private Long durationSeconds;
 @Column(name="waveform_json",columnDefinition="TEXT")private String waveformJson;
 @Column(name="copyright_owner",length=300)private String copyrightOwner;
 @Column(name="license_type",length=100)private String licenseType;
 @Column(name="usage_permission",length=200)private String usagePermission;
 @Column(name="attribution_required")private Boolean attributionRequired;
 @Enumerated(EnumType.STRING)@Column(name="consent_status",length=20)private ConsentStatus consentStatus;
 @Column(name="consent_record_id",length=100)private String consentRecordId;
 @Column(name="signed_url_expires_at")private Instant signedUrlExpiresAt;
 @Column(name="quota_bucket_key",length=200)private String quotaBucketKey;

 public UUID getId(){return id;}public void setId(UUID v){id=v;}public String getOwnerId(){return ownerId;}public void setOwnerId(String v){ownerId=v;}public MediaType getType(){return type;}public void setType(MediaType v){type=v;}
 public MediaStatus getStatus(){return status;}public void setStatus(MediaStatus v){status=v;}public ThumbnailStatus getThumbnailStatus(){return thumbnailStatus;}public void setThumbnailStatus(ThumbnailStatus v){thumbnailStatus=v;}
 public String getOriginalFilename(){return originalFilename;}public void setOriginalFilename(String v){originalFilename=v;}public String getContentType(){return contentType;}public void setContentType(String v){contentType=v;}
 public long getSizeBytes(){return sizeBytes;}public void setSizeBytes(long v){sizeBytes=v;}public String getChecksum(){return checksum;}public void setChecksum(String v){checksum=v;}public String getStorageKey(){return storageKey;}public void setStorageKey(String v){storageKey=v;}
 public String getThumbnailKey(){return thumbnailKey;}public void setThumbnailKey(String v){thumbnailKey=v;}public Integer getWidth(){return width;}public void setWidth(Integer v){width=v;}public Integer getHeight(){return height;}public void setHeight(Integer v){height=v;}
 public Long getDurationMs(){return durationMs;}public void setDurationMs(Long v){durationMs=v;}public String getAltText(){return altText;}public void setAltText(String v){altText=v;}public String getAggregateType(){return aggregateType;}public void setAggregateType(String v){aggregateType=v;}
 public String getAggregateId(){return aggregateId;}public void setAggregateId(String v){aggregateId=v;}public String getFailureReason(){return failureReason;}public void setFailureReason(String v){failureReason=v;}
 public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}public Instant getUpdatedAt(){return updatedAt;}public void setUpdatedAt(Instant v){updatedAt=v;}public Instant getDeletedAt(){return deletedAt;}public void setDeletedAt(Instant v){deletedAt=v;}
 public long getVersion(){return version;}public void setVersion(long v){version=v;}
 public MediaUsageType getUsageType(){return usageType;}public void setUsageType(MediaUsageType v){usageType=v;}
 public Long getDurationSeconds(){return durationSeconds;}public void setDurationSeconds(Long v){durationSeconds=v;}
 public String getWaveformJson(){return waveformJson;}public void setWaveformJson(String v){waveformJson=v;}
 public String getCopyrightOwner(){return copyrightOwner;}public void setCopyrightOwner(String v){copyrightOwner=v;}
 public String getLicenseType(){return licenseType;}public void setLicenseType(String v){licenseType=v;}
 public String getUsagePermission(){return usagePermission;}public void setUsagePermission(String v){usagePermission=v;}
 public Boolean getAttributionRequired(){return attributionRequired;}public void setAttributionRequired(Boolean v){attributionRequired=v;}
 public ConsentStatus getConsentStatus(){return consentStatus;}public void setConsentStatus(ConsentStatus v){consentStatus=v;}
 public String getConsentRecordId(){return consentRecordId;}public void setConsentRecordId(String v){consentRecordId=v;}
 public Instant getSignedUrlExpiresAt(){return signedUrlExpiresAt;}public void setSignedUrlExpiresAt(Instant v){signedUrlExpiresAt=v;}
 public String getQuotaBucketKey(){return quotaBucketKey;}public void setQuotaBucketKey(String v){quotaBucketKey=v;}
}
