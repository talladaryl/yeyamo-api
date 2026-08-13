package com.yeyamo_mobile.api.content_service.infrastructure.persistence;
import java.time.Instant;import java.util.*;import jakarta.persistence.*;import com.yeyamo_mobile.api.content_service.domain.model.*;
import com.yeyamo_mobile.shared.geography.GeographicFields;
@Entity @Table(name="content_posts")
public class PostEntity{
 @Id private UUID id;@Column(name="author_id",nullable=false,length=100)private String authorId;@Column(columnDefinition="TEXT")private String caption;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private PostStatus status;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private PostVisibility visibility;
 @Column(name="catalog_asset_id")private UUID catalogAssetId;@Enumerated(EnumType.STRING)@Column(name="reference_type",nullable=false,length=32)private PostReferenceType referenceType=PostReferenceType.NONE;@Column(name="reference_id",length=100)private String referenceId;
 @ElementCollection(fetch=FetchType.EAGER)@CollectionTable(name="content_post_media",joinColumns=@JoinColumn(name="post_id"))@Column(name="media_id",nullable=false)@OrderColumn(name="display_order")private List<UUID> mediaIds=new ArrayList<>();
 @ElementCollection(fetch=FetchType.EAGER)@CollectionTable(name="content_post_hashtags",joinColumns=@JoinColumn(name="post_id"))@Column(name="hashtag",nullable=false,length=50)private Set<String> hashtags=new LinkedHashSet<>();
 @Embedded private GeographicFields geography;
 @Column(name="created_at",nullable=false)private Instant createdAt;@Column(name="updated_at",nullable=false)private Instant updatedAt;@Column(name="published_at")private Instant publishedAt;
 @Column(name="archived_at")private Instant archivedAt;@Column(name="deleted_at")private Instant deletedAt;@Version private long version;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public String getAuthorId(){return authorId;}public void setAuthorId(String v){authorId=v;}public String getCaption(){return caption;}public void setCaption(String v){caption=v;}
 public PostStatus getStatus(){return status;}public void setStatus(PostStatus v){status=v;}public PostVisibility getVisibility(){return visibility;}public void setVisibility(PostVisibility v){visibility=v;}public UUID getCatalogAssetId(){return catalogAssetId;}public void setCatalogAssetId(UUID v){catalogAssetId=v;}public PostReferenceType getReferenceType(){return referenceType;}public void setReferenceType(PostReferenceType v){referenceType=v;}public String getReferenceId(){return referenceId;}public void setReferenceId(String v){referenceId=v;}
 public List<UUID> getMediaIds(){return mediaIds;}public void setMediaIds(List<UUID> v){mediaIds=v;}public Set<String> getHashtags(){return hashtags;}public void setHashtags(Set<String> v){hashtags=v;}
 public GeographicFields getGeography(){return geography;}public void setGeography(GeographicFields v){geography=v;}
 public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}public Instant getUpdatedAt(){return updatedAt;}public void setUpdatedAt(Instant v){updatedAt=v;}public Instant getPublishedAt(){return publishedAt;}public void setPublishedAt(Instant v){publishedAt=v;}
 public Instant getArchivedAt(){return archivedAt;}public void setArchivedAt(Instant v){archivedAt=v;}public Instant getDeletedAt(){return deletedAt;}public void setDeletedAt(Instant v){deletedAt=v;}public long getVersion(){return version;}public void setVersion(long v){version=v;}
}
