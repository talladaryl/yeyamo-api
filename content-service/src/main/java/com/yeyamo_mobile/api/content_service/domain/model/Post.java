package com.yeyamo_mobile.api.content_service.domain.model;
import java.time.Instant;import java.util.*;
import com.yeyamo_mobile.shared.geography.GeographicFields;
public class Post{
 private UUID id;private String authorId;private String caption;private PostStatus status;private PostVisibility visibility;
 private UUID catalogAssetId;private PostReferenceType referenceType=PostReferenceType.NONE;private String referenceId;private List<UUID> mediaIds=new ArrayList<>();private Set<String> hashtags=new LinkedHashSet<>();
 private GeographicFields geography;
 private Instant createdAt;private Instant updatedAt;private Instant publishedAt;private Instant archivedAt;private Instant deletedAt;private long version;
 public static Post draft(String authorId,String caption,PostVisibility visibility,UUID catalogAssetId,Collection<UUID> mediaIds,Collection<String> hashtags){
  if(authorId==null||authorId.isBlank())throw new IllegalArgumentException("authorId is required");Post p=new Post();p.id=UUID.randomUUID();p.authorId=authorId;
  p.status=PostStatus.DRAFT;p.createdAt=Instant.now();p.updatedAt=p.createdAt;p.apply(caption,visibility,catalogAssetId,mediaIds,hashtags);return p;}
 public void updateDraft(String caption,PostVisibility visibility,UUID catalogAssetId,Collection<UUID> mediaIds,Collection<String> hashtags){
  requireStatus(PostStatus.DRAFT,"Only a draft can be edited");apply(caption,visibility,catalogAssetId,mediaIds,hashtags);}
 public void publish(){requireStatus(PostStatus.DRAFT,"Only a draft can be published");if((caption==null||caption.isBlank())&&mediaIds.isEmpty())throw new IllegalStateException("A post requires a caption or media");
  status=PostStatus.PUBLISHED;publishedAt=Instant.now();updatedAt=publishedAt;}
 public void changeVisibility(PostVisibility visibility){if(status!=PostStatus.DRAFT&&status!=PostStatus.PUBLISHED)throw new IllegalStateException("Visibility cannot be changed");
  this.visibility=visibility==null?PostVisibility.PUBLIC:visibility;updatedAt=Instant.now();}
 public void archive(){requireStatus(PostStatus.PUBLISHED,"Only a published post can be archived");status=PostStatus.ARCHIVED;archivedAt=Instant.now();updatedAt=archivedAt;}
 public void delete(){if(status==PostStatus.DELETED)return;status=PostStatus.DELETED;deletedAt=Instant.now();updatedAt=deletedAt;}
 public boolean publiclyVisible(){return status==PostStatus.PUBLISHED&&visibility==PostVisibility.PUBLIC;}
 private void apply(String caption,PostVisibility visibility,UUID catalogAssetId,Collection<UUID> mediaIds,Collection<String> hashtags){
  this.caption=trim(caption);if(this.caption!=null&&this.caption.length()>5000)throw new IllegalArgumentException("caption exceeds 5000 characters");
  this.visibility=visibility==null?PostVisibility.PUBLIC:visibility;this.catalogAssetId=catalogAssetId;this.mediaIds=uniqueMedia(mediaIds);this.hashtags=normalizeTags(hashtags);updatedAt=Instant.now();}
 public void reference(PostReferenceType type,String id){referenceType=type==null?PostReferenceType.NONE:type;referenceId=id==null||id.isBlank()?null:id.trim();if(referenceType==PostReferenceType.NONE&&referenceId!=null)throw new IllegalArgumentException("NONE reference cannot have id");if(referenceType!=PostReferenceType.NONE&&referenceId==null)throw new IllegalArgumentException("Reference id is required");if(referenceType==PostReferenceType.PLACE&&catalogAssetId==null)try{catalogAssetId=UUID.fromString(referenceId);}catch(IllegalArgumentException ignored){}}
 private List<UUID> uniqueMedia(Collection<UUID> values){LinkedHashSet<UUID> ids=new LinkedHashSet<>();if(values!=null)values.stream().filter(Objects::nonNull).forEach(ids::add);
  if(ids.size()>10)throw new IllegalArgumentException("A post supports at most 10 media");return new ArrayList<>(ids);}
 private Set<String> normalizeTags(Collection<String> values){LinkedHashSet<String> tags=new LinkedHashSet<>();if(values!=null)for(String value:values){String tag=trim(value);
   if(tag==null)continue;tag=tag.startsWith("#")?tag.substring(1):tag;tag=tag.toLowerCase(Locale.ROOT);if(!tag.matches("[\\p{L}\\p{N}_-]{1,50}"))throw new IllegalArgumentException("Invalid hashtag: "+tag);tags.add(tag);}
  if(tags.size()>20)throw new IllegalArgumentException("A post supports at most 20 hashtags");return tags;}
 private void requireStatus(PostStatus expected,String message){if(status!=expected)throw new IllegalStateException(message);}
 private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public String getAuthorId(){return authorId;}public void setAuthorId(String v){authorId=v;}public String getCaption(){return caption;}public void setCaption(String v){caption=v;}
 public PostStatus getStatus(){return status;}public void setStatus(PostStatus v){status=v;}public PostVisibility getVisibility(){return visibility;}public void setVisibility(PostVisibility v){visibility=v;}
 public UUID getCatalogAssetId(){return catalogAssetId;}public void setCatalogAssetId(UUID v){catalogAssetId=v;}public PostReferenceType getReferenceType(){return referenceType;}public void setReferenceType(PostReferenceType v){referenceType=v==null?PostReferenceType.NONE:v;}public String getReferenceId(){return referenceId;}public void setReferenceId(String v){referenceId=v;}public List<UUID> getMediaIds(){return List.copyOf(mediaIds);}public void setMediaIds(Collection<UUID> v){mediaIds=v==null?new ArrayList<>():new ArrayList<>(v);}
 public Set<String> getHashtags(){return Collections.unmodifiableSet(new LinkedHashSet<>(hashtags));}public void setHashtags(Collection<String> v){hashtags=v==null?new LinkedHashSet<>():new LinkedHashSet<>(v);}
 public GeographicFields getGeography(){return geography;}public void setGeography(GeographicFields v){geography=v;}
 public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}public Instant getUpdatedAt(){return updatedAt;}public void setUpdatedAt(Instant v){updatedAt=v;}
 public Instant getPublishedAt(){return publishedAt;}public void setPublishedAt(Instant v){publishedAt=v;}public Instant getArchivedAt(){return archivedAt;}public void setArchivedAt(Instant v){archivedAt=v;}
 public Instant getDeletedAt(){return deletedAt;}public void setDeletedAt(Instant v){deletedAt=v;}public long getVersion(){return version;}public void setVersion(long v){version=v;}
}
