package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.util.*;import org.springframework.http.*;import org.springframework.security.core.*;import org.springframework.validation.annotation.Validated;import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.interaction_service.application.*;import com.yeyamo_mobile.api.interaction_service.domain.model.RelationType;
import io.swagger.v3.oas.annotations.*;import io.swagger.v3.oas.annotations.security.SecurityRequirement;import io.swagger.v3.oas.annotations.tags.Tag;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
@RestController @RequestMapping("/api/v1/interactions")@Validated @Tag(name="Interactions",description="Likes, comments, favorites and shares")
public class InteractionController{
 private final InteractionCommandService commands;private final InteractionQueryService queries;public InteractionController(InteractionCommandService c,InteractionQueryService q){commands=c;queries=q;}
 @PutMapping("/posts/{postId}/like")@Operation(summary="Like a post",security=@SecurityRequirement(name="bearerAuth"))
 public CommandResponse like(@PathVariable UUID postId,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){return CommandResponse.from(commands.addRelation(postId,auth.getName(),RelationType.LIKE,key,correlation));}
 @DeleteMapping("/posts/{postId}/like")@Operation(summary="Remove a like",security=@SecurityRequirement(name="bearerAuth"))
 public CommandResponse unlike(@PathVariable UUID postId,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){return CommandResponse.from(commands.removeRelation(postId,auth.getName(),RelationType.LIKE,key,correlation));}
 @PutMapping("/posts/{postId}/favorite")@Operation(summary="Save a post",security=@SecurityRequirement(name="bearerAuth"))
 public CommandResponse favorite(@PathVariable UUID postId,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){return CommandResponse.from(commands.addRelation(postId,auth.getName(),RelationType.FAVORITE,key,correlation));}
 @DeleteMapping("/posts/{postId}/favorite")@Operation(summary="Remove a saved post",security=@SecurityRequirement(name="bearerAuth"))
 public CommandResponse unfavorite(@PathVariable UUID postId,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){return CommandResponse.from(commands.removeRelation(postId,auth.getName(),RelationType.FAVORITE,key,correlation));}
 @PostMapping("/posts/{postId}/comments")@ResponseStatus(HttpStatus.CREATED)@Operation(summary="Comment a post",security=@SecurityRequirement(name="bearerAuth"))
 public CommentResponse comment(@PathVariable UUID postId,@Valid@RequestBody CommentRequest request,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
  return CommentResponse.from(commands.addComment(postId,request.parentId(),auth.getName(),request.body(),key,correlation));}
 @PutMapping("/comments/{id}")@Operation(summary="Edit a comment",security=@SecurityRequirement(name="bearerAuth"))
 public CommentResponse edit(@PathVariable UUID id,@Valid@RequestBody CommentRequest request,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
  return CommentResponse.from(commands.updateComment(id,auth.getName(),admin(auth),request.body(),key,correlation));}
 @DeleteMapping("/comments/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)@Operation(summary="Soft-delete a comment",security=@SecurityRequirement(name="bearerAuth"))
 public void delete(@PathVariable UUID id,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){commands.deleteComment(id,auth.getName(),admin(auth),key,correlation);}
 @PostMapping("/posts/{postId}/shares")@ResponseStatus(HttpStatus.CREATED)@Operation(summary="Share a post",security=@SecurityRequirement(name="bearerAuth"))
 public ShareResponse share(@PathVariable UUID postId,@Valid@RequestBody ShareRequest request,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
  return ShareResponse.from(commands.share(postId,auth.getName(),request.channel(),key,correlation));}
 @GetMapping("/posts/{postId}/summary")@Operation(summary="Read post interaction counters")
 public InteractionSummary summary(@PathVariable UUID postId,Authentication auth){return queries.summary(postId,auth==null?null:auth.getName());}
 @GetMapping("/posts/{postId}/comments")@Operation(summary="Read active comments")
 public List<CommentResponse> comments(@PathVariable UUID postId,@RequestParam(defaultValue="50")@Min(1)@Max(100)int limit){return queries.comments(postId,limit).stream().map(CommentResponse::from).toList();}
 
 // ─── REVIEWS ─────────────────────────────────────────────────────────────────
 
 @PostMapping("/places/{placeId}/reviews")
 @ResponseStatus(HttpStatus.CREATED)
 @Operation(summary="Create a review for a place",security=@SecurityRequirement(name="bearerAuth"))
 public com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse createReview(
  @PathVariable UUID placeId,
  @Valid@RequestBody com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewRequest request,
  @RequestHeader("Idempotency-Key")@NotBlank String key,
  @RequestHeader(value="X-Correlation-Id",required=false)String correlation,
  Authentication auth){
  var review=commands.createReview(placeId,auth.getName(),request.rating().shortValue(),request.comment(),key,correlation);
  return com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse.from(review);
 }
 
 @PutMapping("/reviews/{id}")
 @Operation(summary="Update your review",security=@SecurityRequirement(name="bearerAuth"))
 public com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse updateReview(
  @PathVariable UUID id,
  @Valid@RequestBody com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewRequest request,
  @RequestHeader("Idempotency-Key")@NotBlank String key,
  @RequestHeader(value="X-Correlation-Id",required=false)String correlation,
  Authentication auth){
  var review=commands.updateReview(id,auth.getName(),request.rating().shortValue(),request.comment(),key,correlation);
  return com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse.from(review);
 }
 
 @DeleteMapping("/reviews/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 @Operation(summary="Delete a review (author or moderator only)",security=@SecurityRequirement(name="bearerAuth"))
 public void deleteReview(
  @PathVariable UUID id,
  @RequestHeader("Idempotency-Key")@NotBlank String key,
  @RequestHeader(value="X-Correlation-Id",required=false)String correlation,
  Authentication auth){
  commands.deleteReview(id,auth.getName(),admin(auth),key,correlation);
 }
 
 @GetMapping("/places/{placeId}/reviews")
 @Operation(summary="Get reviews for a place")
 public List<com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse> placeReviews(
  @PathVariable UUID placeId,
  @RequestParam(defaultValue="50")@Min(1)@Max(100)int limit){
  return queries.reviewsByPlace(placeId,limit).stream()
   .map(com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse::from).toList();
 }
 
 @GetMapping("/users/{userId}/reviews")
 @Operation(summary="Get reviews by a user")
 public List<com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse> userReviews(
  @PathVariable String userId,
  @RequestParam(defaultValue="50")@Min(1)@Max(100)int limit){
  return queries.reviewsByUser(userId,limit).stream()
   .map(com.yeyamo_mobile.api.interaction_service.interfaces.rest.ReviewResponse::from).toList();
 }
 
 private boolean admin(Authentication a){return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(v->v.equals("ROLE_ADMIN")||v.equals("ROLE_SUPER_ADMIN")||v.equals("ROLE_MODERATOR"));}
}
