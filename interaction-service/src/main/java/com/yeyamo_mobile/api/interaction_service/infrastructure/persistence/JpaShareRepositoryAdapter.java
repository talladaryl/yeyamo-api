package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.interaction_service.domain.model.PostShare;import com.yeyamo_mobile.api.interaction_service.domain.port.ShareRepository;
@Component
public class JpaShareRepositoryAdapter implements ShareRepository{
 private final SpringShareRepository repo;public JpaShareRepositoryAdapter(SpringShareRepository r){repo=r;}public PostShare save(PostShare s){ShareEntity e=new ShareEntity();e.setId(s.id());e.setPostId(s.postId());e.setUserId(s.userId());e.setChannel(s.channel());e.setCreatedAt(s.createdAt());return domain(repo.save(e));}
 public Optional<PostShare>findById(UUID id){return repo.findById(id).map(this::domain);}public long countByPost(UUID id){return repo.countByPostId(id);}private PostShare domain(ShareEntity e){return new PostShare(e.getId(),e.getPostId(),e.getUserId(),e.getChannel(),e.getCreatedAt());}}
