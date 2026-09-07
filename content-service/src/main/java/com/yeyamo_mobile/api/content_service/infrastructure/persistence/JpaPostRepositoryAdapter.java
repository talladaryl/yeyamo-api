package com.yeyamo_mobile.api.content_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.PageRequest;import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.content_service.domain.model.*;import com.yeyamo_mobile.api.content_service.domain.port.PostRepository;
@Component
public class JpaPostRepositoryAdapter implements PostRepository{
 private final SpringPostRepository repo;public JpaPostRepositoryAdapter(SpringPostRepository r){repo=r;}
 public Post save(Post p){return domain(repo.save(entity(p)));}public Optional<Post> findById(UUID id){return repo.findById(id).map(this::domain);}
 public List<Post> findByAuthor(String a,int l){return repo.findByAuthorIdAndStatusNotOrderByUpdatedAtDesc(a,PostStatus.DELETED,PageRequest.of(0,l)).stream().map(this::domain).toList();}
 public List<Post> findPublishedByHashtag(String h,int l){return repo.byHashtag(h,PostStatus.PUBLISHED,PostVisibility.PUBLIC,PageRequest.of(0,l)).stream().map(this::domain).toList();}
 public List<Post> findPublishedByCatalogAsset(UUID id,int l){return repo.findByCatalogAssetIdAndStatusAndVisibilityOrderByPublishedAtDesc(id,PostStatus.PUBLISHED,PostVisibility.PUBLIC,PageRequest.of(0,l)).stream().map(this::domain).toList();}
 private PostEntity entity(Post p){PostEntity e=new PostEntity();e.setId(p.getId());e.setAuthorId(p.getAuthorId());e.setCaption(p.getCaption());e.setStatus(p.getStatus());e.setVisibility(p.getVisibility());e.setCatalogAssetId(p.getCatalogAssetId());e.setReferenceType(p.getReferenceType());e.setReferenceId(p.getReferenceId());e.setTargetType(p.getTargetType());e.setTargetId(p.getTargetId());
  e.setMediaIds(new ArrayList<>(p.getMediaIds()));e.setHashtags(new LinkedHashSet<>(p.getHashtags()));e.setGeography(p.getGeography());e.setCreatedAt(p.getCreatedAt());e.setUpdatedAt(p.getUpdatedAt());e.setPublishedAt(p.getPublishedAt());e.setArchivedAt(p.getArchivedAt());e.setDeletedAt(p.getDeletedAt());e.setVersion(p.getVersion());return e;}
 private Post domain(PostEntity e){Post p=new Post();p.setId(e.getId());p.setAuthorId(e.getAuthorId());p.setCaption(e.getCaption());p.setStatus(e.getStatus());p.setVisibility(e.getVisibility());p.setCatalogAssetId(e.getCatalogAssetId());p.setReferenceType(e.getReferenceType());p.setReferenceId(e.getReferenceId());p.setTargetType(e.getTargetType());p.setTargetId(e.getTargetId());
  p.setMediaIds(e.getMediaIds());p.setHashtags(e.getHashtags());p.setGeography(e.getGeography());p.setCreatedAt(e.getCreatedAt());p.setUpdatedAt(e.getUpdatedAt());p.setPublishedAt(e.getPublishedAt());p.setArchivedAt(e.getArchivedAt());p.setDeletedAt(e.getDeletedAt());p.setVersion(e.getVersion());return p;}
}
