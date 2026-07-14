package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.PageRequest;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.interaction_service.domain.model.*;import com.yeyamo_mobile.api.interaction_service.domain.port.RelationRepository;
@Component
public class JpaRelationRepositoryAdapter implements RelationRepository{
 private final SpringRelationRepository repo;public JpaRelationRepositoryAdapter(SpringRelationRepository r){repo=r;}
 public Optional<PostRelation>find(UUID p,String u,RelationType t){return repo.findByPostIdAndUserIdAndType(p,u,t).map(this::domain);}public PostRelation save(PostRelation r){RelationEntity e=new RelationEntity();e.setId(r.id());e.setPostId(r.postId());e.setUserId(r.userId());e.setType(r.type());e.setCreatedAt(r.createdAt());return domain(repo.save(e));}
 public void delete(PostRelation r){repo.deleteById(r.id());}public long count(UUID p,RelationType t){return repo.countByPostIdAndType(p,t);}
 public List<PostRelation>findFavorites(String u,int l){return repo.findByUserIdAndTypeOrderByCreatedAtDesc(u,RelationType.FAVORITE,PageRequest.of(0,l)).stream().map(this::domain).toList();}
 private PostRelation domain(RelationEntity e){return new PostRelation(e.getId(),e.getPostId(),e.getUserId(),e.getType(),e.getCreatedAt());}
}
