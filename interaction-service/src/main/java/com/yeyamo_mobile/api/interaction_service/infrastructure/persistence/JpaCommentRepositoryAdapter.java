package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.PageRequest;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.interaction_service.domain.model.*;import com.yeyamo_mobile.api.interaction_service.domain.port.CommentRepository;
@Component
public class JpaCommentRepositoryAdapter implements CommentRepository{
 private final SpringCommentRepository repo;public JpaCommentRepositoryAdapter(SpringCommentRepository r){repo=r;}public Comment save(Comment c){return domain(repo.save(entity(c)));}public Optional<Comment>findById(UUID id){return repo.findById(id).map(this::domain);}
 public List<Comment>findActiveByPost(UUID id,int l){return repo.findByPostIdAndStatusOrderByCreatedAtAsc(id,CommentStatus.ACTIVE,PageRequest.of(0,l)).stream().map(this::domain).toList();}public long countActiveByPost(UUID id){return repo.countByPostIdAndStatus(id,CommentStatus.ACTIVE);}
 private CommentEntity entity(Comment c){CommentEntity e=new CommentEntity();e.setId(c.getId());e.setPostId(c.getPostId());e.setParentId(c.getParentId());e.setAuthorId(c.getAuthorId());e.setBody(c.getBody());e.setStatus(c.getStatus());e.setCreatedAt(c.getCreatedAt());e.setUpdatedAt(c.getUpdatedAt());e.setDeletedAt(c.getDeletedAt());e.setVersion(c.getVersion());return e;}
 private Comment domain(CommentEntity e){Comment c=new Comment();c.setId(e.getId());c.setPostId(e.getPostId());c.setParentId(e.getParentId());c.setAuthorId(e.getAuthorId());c.setBody(e.getBody());c.setStatus(e.getStatus());c.setCreatedAt(e.getCreatedAt());c.setUpdatedAt(e.getUpdatedAt());c.setDeletedAt(e.getDeletedAt());c.setVersion(e.getVersion());return c;}
}
