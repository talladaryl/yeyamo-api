package com.yeyamo_mobile.api.interaction_service.application;
import java.util.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.interaction_service.application.port.InteractionCachePort;import com.yeyamo_mobile.api.interaction_service.domain.model.*;import com.yeyamo_mobile.api.interaction_service.domain.port.*;
@Service
public class InteractionQueryService{
 private final RelationRepository relations;private final CommentRepository comments;private final ShareRepository shares;private final CheckInRepository checks;
 private final com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringReviewRepository reviews;
 private final InteractionCachePort cache;
 public InteractionQueryService(RelationRepository r,CommentRepository c,ShareRepository s,CheckInRepository i,
  com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringReviewRepository rev,
  InteractionCachePort cache){
  relations=r;comments=c;shares=s;checks=i;reviews=rev;this.cache=cache;
 }
 @Transactional(readOnly=true)public InteractionSummary summary(UUID postId,String viewer){InteractionSummary.Counts counts=cache.getCounts(postId).orElseGet(()->{var c=new InteractionSummary.Counts(relations.count(postId,RelationType.LIKE),comments.countActiveByPost(postId),shares.countByPost(postId));cache.putCounts(postId,c);return c;});
  boolean liked=viewer!=null&&relations.find(postId,viewer,RelationType.LIKE).isPresent();boolean favorite=viewer!=null&&relations.find(postId,viewer,RelationType.FAVORITE).isPresent();
  return new InteractionSummary(postId,counts.likes(),counts.comments(),counts.shares(),liked,favorite);}
 @Transactional(readOnly=true)public List<Comment> comments(UUID postId,int limit){return comments.findActiveByPost(postId,cap(limit));}
 @Transactional(readOnly=true)public List<PostRelation> favorites(String user,int limit){return relations.findFavorites(user,cap(limit));}
 @Transactional(readOnly=true)public List<CheckIn> checkIns(String user,int limit){return checks.findByUser(user,cap(limit));}
 
 // ─── REVIEWS ─────────────────────────────────────────────────────────────────
 
 @Transactional(readOnly=true)
 public List<com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity> reviewsByPlace(UUID placeId,int limit){
  return reviews.findByPlaceIdOrderByCreatedAtDesc(placeId,org.springframework.data.domain.PageRequest.of(0,cap(limit)));
 }
 
 @Transactional(readOnly=true)
 public List<com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity> reviewsByUser(String userId,int limit){
  return reviews.findByUserIdOrderByCreatedAtDesc(userId,org.springframework.data.domain.PageRequest.of(0,cap(limit))).stream().filter(review->"ACTIVE".equals(review.getStatus())).toList();
 }
 
 private int cap(int n){return Math.max(1,Math.min(n,100));}
}
