package com.yeyamo_mobile.api.interaction_service.application;
import java.util.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.interaction_service.application.port.*;import com.yeyamo_mobile.api.interaction_service.domain.model.*;import com.yeyamo_mobile.api.interaction_service.domain.port.*;
@Service
public class InteractionCommandService{
 private final RelationRepository relations;private final CommentRepository comments;private final ShareRepository shares;private final CheckInRepository checkIns;
 private final com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringReviewRepository reviews;
 private final CommandReceiptPort receipts;private final InteractionOutboxPort outbox;private final InteractionCachePort cache;
 public InteractionCommandService(RelationRepository r,CommentRepository c,ShareRepository s,CheckInRepository i,
  com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringReviewRepository rev,
  CommandReceiptPort p,InteractionOutboxPort o,InteractionCachePort cache){
  relations=r;comments=c;shares=s;checkIns=i;reviews=rev;receipts=p;outbox=o;this.cache=cache;}
 @Transactional public CommandResult addRelation(UUID postId,String actor,RelationType type,String key,String correlation){String op=type+"_ADD:"+postId;Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return result(replay.get(),true);Optional<PostRelation> existing=relations.find(postId,actor,type);PostRelation relation=existing.orElseGet(()->relations.save(PostRelation.create(postId,actor,type)));
  boolean changed=existing.isEmpty();if(changed){outbox.append(event(type,true),"post",postId.toString(),actor,correlation,Map.of("postId",postId,"userId",actor));cache.evict(postId);}
  CommandReceipt receipt=receipts.save(CommandReceipt.create(key,actor,op,relation.id(),changed));return result(receipt,false);}
 @Transactional public CommandResult removeRelation(UUID postId,String actor,RelationType type,String key,String correlation){String op=type+"_REMOVE:"+postId;Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return result(replay.get(),true);Optional<PostRelation> existing=relations.find(postId,actor,type);existing.ifPresent(relations::delete);boolean changed=existing.isPresent();
  if(changed){outbox.append(event(type,false),"post",postId.toString(),actor,correlation,Map.of("postId",postId,"userId",actor));cache.evict(postId);}
  CommandReceipt receipt=receipts.save(CommandReceipt.create(key,actor,op,existing.map(PostRelation::id).orElse(postId),changed));return result(receipt,false);}
 @Transactional public Comment addComment(UUID postId,UUID parentId,String actor,String body,String key,String correlation){String op="COMMENT_ADD:"+postId;Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return comments.findById(replay.get().resultId()).orElseThrow();if(parentId!=null){Comment parent=comments.findById(parentId).filter(c->c.getStatus()==CommentStatus.ACTIVE)
   .orElseThrow(()->new InteractionException("PARENT_COMMENT_NOT_FOUND","Parent comment not found"));if(!parent.getPostId().equals(postId))throw new InteractionException("COMMENT_POST_MISMATCH","Parent comment belongs to another post");}
  Comment comment=comments.save(Comment.create(postId,parentId,actor,body));receipts.save(CommandReceipt.create(key,actor,op,comment.getId(),true));
  outbox.append("interaction.comment.created","comment",comment.getId().toString(),actor,correlation,Map.of("commentId",comment.getId(),"postId",postId,"authorId",actor));cache.evict(postId);return comment;}
 @Transactional public Comment updateComment(UUID id,String actor,boolean admin,String body,String key,String correlation){String op="COMMENT_UPDATE:"+id;Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return requiredComment(id);Comment c=ownedComment(id,actor,admin);c.update(body);c=comments.save(c);receipts.save(CommandReceipt.create(key,actor,op,id,true));
  outbox.append("interaction.comment.updated","comment",id.toString(),actor,correlation,Map.of("commentId",id,"postId",c.getPostId()));return c;}
 @Transactional public void deleteComment(UUID id,String actor,boolean admin,String key,String correlation){String op="COMMENT_DELETE:"+id;if(receipts.find(key,actor,op).isPresent())return;Comment c=ownedComment(id,actor,admin);c.delete();comments.save(c);
  receipts.save(CommandReceipt.create(key,actor,op,id,true));outbox.append("interaction.comment.deleted","comment",id.toString(),actor,correlation,Map.of("commentId",id,"postId",c.getPostId()));cache.evict(c.getPostId());}
 @Transactional public PostShare share(UUID postId,String actor,String channel,String key,String correlation){String op="SHARE:"+postId;Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return shares.findById(replay.get().resultId()).orElseThrow();PostShare share=shares.save(PostShare.create(postId,actor,channel));receipts.save(CommandReceipt.create(key,actor,op,share.id(),true));
  outbox.append("interaction.post.shared","share",share.id().toString(),actor,correlation,Map.of("shareId",share.id(),"postId",postId,"userId",actor,"channel",share.channel()));cache.evict(postId);return share;}
 @Transactional public CheckIn checkIn(UUID assetId,String actor,Double lat,Double lng,boolean visible,String key,String correlation){String op="CHECKIN:"+assetId;Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return checkIns.findById(replay.get().resultId()).orElseThrow();CheckIn check=checkIns.save(CheckIn.create(assetId,actor,lat,lng,visible));receipts.save(CommandReceipt.create(key,actor,op,check.id(),true));
  Map<String,Object> payload=new LinkedHashMap<>();payload.put("checkInId",check.id());payload.put("catalogAssetId",assetId);payload.put("userId",actor);payload.put("visible",visible);payload.put("occurredAt",check.occurredAt());
  outbox.append("interaction.checkin.created","checkin",check.id().toString(),actor,correlation,payload);return check;}
 // ─── REVIEWS ─────────────────────────────────────────────────────────────────
 
 @Transactional 
 public com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity createReview(
   UUID placeId,String actor,short rating,String comment,String key,String correlation){
  String op="REVIEW_CREATE:"+placeId;
  Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return reviews.findById(replay.get().resultId()).orElseThrow();
  
  // Check for duplicate (409 Conflict)
  if(reviews.existsByUserIdAndPlaceId(actor,placeId)){
   throw new InteractionException("REVIEW_ALREADY_EXISTS",
    "You have already reviewed this place. Use PUT to update your review.");
  }
  
  var review=new com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity();
  review.setId(UUID.randomUUID());
  review.setUserId(actor);
  review.setPlaceId(placeId);
  review.setRating(rating);
  review.setComment(comment);
  review.setCreatedAt(java.time.Instant.now());
  review.setUpdatedAt(java.time.Instant.now());
  
  review=reviews.save(review);
  receipts.save(CommandReceipt.create(key,actor,op,review.getId(),true));
  
  outbox.append("interaction.review.created","review",review.getId().toString(),actor,correlation,
   Map.of("reviewId",review.getId(),"placeId",placeId,"userId",actor,"rating",rating));
  
  return review;
 }
 
 @Transactional
 public com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity updateReview(
   UUID id,String actor,short rating,String comment,String key,String correlation){
  String op="REVIEW_UPDATE:"+id;
  Optional<CommandReceipt> replay=receipts.find(key,actor,op);
  if(replay.isPresent())return requiredReview(id);
  
  var review=ownedReview(id,actor,false);
  review.setRating(rating);
  review.setComment(comment);
  review.setUpdatedAt(java.time.Instant.now());
  
  review=reviews.save(review);
  receipts.save(CommandReceipt.create(key,actor,op,id,true));
  
  outbox.append("interaction.review.updated","review",id.toString(),actor,correlation,
   Map.of("reviewId",id,"placeId",review.getPlaceId(),"rating",rating));
  
  return review;
 }
 
 @Transactional
 public void deleteReview(UUID id,String actor,boolean moderator,String key,String correlation){
  String op="REVIEW_DELETE:"+id;
  if(receipts.find(key,actor,op).isPresent())return;
  
  var review=ownedReview(id,actor,moderator);
  UUID placeId=review.getPlaceId();
  
  reviews.deleteById(id);
  receipts.save(CommandReceipt.create(key,actor,op,id,true));
  
  outbox.append("interaction.review.deleted","review",id.toString(),actor,correlation,
   Map.of("reviewId",id,"placeId",placeId));
 }
 
 private com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity ownedReview(
   UUID id,String actor,boolean moderator){
  var review=requiredReview(id);
  if(!moderator&&!review.getUserId().equals(actor)){
   throw new InteractionException("INTERACTION_FORBIDDEN","Only the review author or a moderator can modify it");
  }
  return review;
 }
 
 private com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.ReviewEntity requiredReview(UUID id){
  return reviews.findById(id)
   .orElseThrow(()->new InteractionException("REVIEW_NOT_FOUND","Review not found"));
 }
 
 // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────
 
 private Comment ownedComment(UUID id,String actor,boolean admin){Comment c=requiredComment(id);if(!admin&&!c.getAuthorId().equals(actor))throw new InteractionException("INTERACTION_FORBIDDEN","Only the comment author can modify it");return c;}
 private Comment requiredComment(UUID id){return comments.findById(id).orElseThrow(()->new InteractionException("COMMENT_NOT_FOUND","Comment not found"));}
 private CommandResult result(CommandReceipt r,boolean replay){return new CommandResult(r.resultId(),r.changed(),replay);}
 private String event(RelationType t,boolean add){return "interaction."+t.name().toLowerCase(Locale.ROOT)+(add?".added":".removed");}
}
