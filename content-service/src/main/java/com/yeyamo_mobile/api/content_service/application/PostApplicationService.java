package com.yeyamo_mobile.api.content_service.application;
import java.util.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.beans.factory.annotation.Autowired;
import com.yeyamo_mobile.api.content_service.infrastructure.outbox.ContentOutboxPort;import com.yeyamo_mobile.api.content_service.application.port.ReferenceVisibilityPort;import com.yeyamo_mobile.api.content_service.domain.model.*;
import com.yeyamo_mobile.api.content_service.domain.port.PostRepository;
import com.yeyamo_mobile.shared.country.CountryConfigClient;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryFeature;
import com.yeyamo_mobile.shared.geography.GeographicFields;
@Service
public class PostApplicationService{
 private final PostRepository repository;private final ContentOutboxPort outbox;private final ReferenceVisibilityPort references;private final CountryConfigClient countries;
 public PostApplicationService(PostRepository r,ContentOutboxPort o,ReferenceVisibilityPort references){this(r,o,references,null);}
 @Autowired public PostApplicationService(PostRepository r,ContentOutboxPort o,ReferenceVisibilityPort references,CountryConfigClient countries){repository=r;outbox=o;this.references=references;this.countries=countries;}
 @Transactional public Post createDraft(String authorId,PostCommand c,String correlationId){validateTarget(c);validateReference(c);validateGeography(c);Post p=Post.draft(authorId,c.caption(),c.visibility(),c.catalogAssetId(),c.mediaIds(),c.hashtags());p.reference(c.referenceType(),c.referenceId());p.culturalTarget(c.targetType(),c.targetId());p.setGeography(geography(c));
  p=repository.save(p);outbox.append("content.post.created",p,correlationId,authorId);return p;}
 @Transactional public Post updateDraft(UUID id,String actorId,boolean admin,PostCommand c,String correlationId){validateTarget(c);validateReference(c);validateGeography(c);Post p=owned(id,actorId,admin);p.updateDraft(c.caption(),c.visibility(),c.catalogAssetId(),c.mediaIds(),c.hashtags());p.reference(c.referenceType(),c.referenceId());p.culturalTarget(c.targetType(),c.targetId());p.setGeography(geography(c));
  p=repository.save(p);outbox.append("content.post.updated",p,correlationId,actorId);return p;}
 @Transactional public Post publish(UUID id,String actorId,boolean admin,String correlationId){Post p=owned(id,actorId,admin);p.publish();p=repository.save(p);outbox.append("content.post.published",p,correlationId,actorId);return p;}
 @Transactional public Post changeVisibility(UUID id,String actorId,boolean admin,PostVisibility visibility,String correlationId){Post p=owned(id,actorId,admin);p.changeVisibility(visibility);p=repository.save(p);
  outbox.append("content.post.visibility_changed",p,correlationId,actorId);return p;}
 @Transactional public Post archive(UUID id,String actorId,boolean admin,String correlationId){Post p=owned(id,actorId,admin);p.archive();p=repository.save(p);outbox.append("content.post.archived",p,correlationId,actorId);return p;}
 @Transactional public void delete(UUID id,String actorId,boolean admin,String correlationId){Post p=owned(id,actorId,admin);p.delete();p=repository.save(p);outbox.append("content.post.deleted",p,correlationId,actorId);}
 @Transactional(readOnly=true)public Post publicPost(UUID id){Post p=required(id);if(!p.publiclyVisible())throw new ContentException("POST_NOT_FOUND","Post not found");return p;}
 @Transactional(readOnly=true)public Post myPost(UUID id,String actorId,boolean admin){return owned(id,actorId,admin);}
 @Transactional(readOnly=true)public List<Post> myPosts(String authorId,int limit){return repository.findByAuthor(authorId,cap(limit));}
 @Transactional(readOnly=true)public List<Post> byHashtag(String hashtag,int limit){return repository.findPublishedByHashtag(normalizeTag(hashtag),cap(limit));}
 @Transactional(readOnly=true)public List<Post> byCatalogAsset(UUID assetId,int limit){return repository.findPublishedByCatalogAsset(assetId,cap(limit));}
 private Post owned(UUID id,String actor,boolean admin){Post p=required(id);if(!admin&&!p.getAuthorId().equals(actor))throw new ContentException("POST_FORBIDDEN","Only the author can modify this post");return p;}
 private void validateTarget(PostCommand command){if((command.targetType()==null)!=(command.targetId()==null))throw new ContentException("CULTURE_TARGET_INVALID","targetType and targetId must be supplied together");if(command.targetType()!=null&&command.referenceType()!=PostReferenceType.CULTURE_CONTENT&&command.referenceType()!=PostReferenceType.NONE)throw new ContentException("CULTURE_TARGET_INVALID","A cultural target requires a CULTURE_CONTENT reference");}
 private void validateReference(PostCommand command){if(command.targetType()!=null)references.requireCultureTarget(command.targetType(),command.targetId());else references.requirePublic(command.referenceType(),command.referenceId());}
 private void validateGeography(PostCommand c){if(c.countryCode()==null||c.countryCode().isBlank())return;if(countries==null)throw new ContentException("COUNTRY_CONFIGURATION_UNAVAILABLE","Country validation is required for geolocated content");try{countries.validateFeature(c.countryCode(),CountryFeature.CONTENT_PUBLISHING);countries.validateCity(c.countryCode(),c.cityId());}catch(CountryConfigClient.CountryConfigException e){throw new ContentException("COUNTRY_CONFIGURATION_REJECTED",e.getMessage());}}
 private GeographicFields geography(PostCommand c){if(c.countryCode()==null||c.countryCode().isBlank())return null;GeographicFields g=new GeographicFields(c.countryCode());g.setLocation(c.adminLevel1Id(),c.adminLevel2Id(),c.cityId(),c.localityId());g.setCoordinates(c.latitude(),c.longitude());g.setLanguageCode(c.languageCode());return g;}
 private Post required(UUID id){return repository.findById(id).filter(p->p.getStatus()!=PostStatus.DELETED).orElseThrow(()->new ContentException("POST_NOT_FOUND","Post not found"));}
 private int cap(int limit){return Math.max(1,Math.min(limit,100));}private String normalizeTag(String v){if(v==null)return "";return (v.startsWith("#")?v.substring(1):v).toLowerCase(Locale.ROOT);}
}
