package com.yeyamo_mobile.api.content_service.interfaces.rest;
import java.util.*;import org.springframework.http.*;import org.springframework.security.core.*;import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.content_service.application.*;import com.yeyamo_mobile.api.content_service.domain.model.PostReferenceType;import io.swagger.v3.oas.annotations.*;import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;import io.swagger.v3.oas.annotations.tags.Tag;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
@RestController @RequestMapping("/api/v1/posts") @Tag(name="Posts",description="Drafts and social publications")
public class PostController{
 private final PostApplicationService service;public PostController(PostApplicationService s){service=s;}
 @Operation(summary="Create a draft",security=@SecurityRequirement(name="bearerAuth"))@PostMapping @ResponseStatus(HttpStatus.CREATED)
 public PostResponse create(@Valid @RequestBody PostRequest r,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
  return PostResponse.from(service.createDraft(auth.getName(),command(r),correlation));}
 @Operation(summary="Update a draft",security=@SecurityRequirement(name="bearerAuth"))@PutMapping("/{id}")
 public PostResponse update(@PathVariable UUID id,@Valid @RequestBody PostRequest r,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
  return PostResponse.from(service.updateDraft(id,auth.getName(),admin(auth),command(r),correlation));}
 @Operation(summary="Publish a draft",security=@SecurityRequirement(name="bearerAuth"))@PostMapping("/{id}/publish")
 public PostResponse publish(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){return PostResponse.from(service.publish(id,auth.getName(),admin(auth),correlation));}
 @Operation(summary="Change visibility",security=@SecurityRequirement(name="bearerAuth"))@PatchMapping("/{id}/visibility")
 public PostResponse visibility(@PathVariable UUID id,@Valid @RequestBody VisibilityRequest r,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
  return PostResponse.from(service.changeVisibility(id,auth.getName(),admin(auth),r.visibility(),correlation));}
 @Operation(summary="Archive a publication",security=@SecurityRequirement(name="bearerAuth"))@PostMapping("/{id}/archive")
 public PostResponse archive(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){return PostResponse.from(service.archive(id,auth.getName(),admin(auth),correlation));}
 @Operation(summary="Soft-delete a post",security=@SecurityRequirement(name="bearerAuth"))@DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){service.delete(id,auth.getName(),admin(auth),correlation);}
 @Operation(summary="Get a public post")@GetMapping("/{id}")public PostResponse publicPost(@PathVariable UUID id){return PostResponse.from(service.publicPost(id));}
 @Operation(summary="List my posts",security=@SecurityRequirement(name="bearerAuth"))@GetMapping("/me")
 public List<PostResponse> mine(@RequestParam(defaultValue="50")@Min(1)@Max(100)int limit,Authentication auth){return service.myPosts(auth.getName(),limit).stream().map(PostResponse::from).toList();}
 @Operation(summary="Get one of my posts",security=@SecurityRequirement(name="bearerAuth"))@GetMapping("/me/{id}")
 public PostResponse mine(@PathVariable UUID id,Authentication auth){return PostResponse.from(service.myPost(id,auth.getName(),admin(auth)));}
 @Operation(summary="Find public posts by hashtag")@GetMapping("/hashtags/{tag}")
 public List<PostResponse> hashtag(@PathVariable String tag,@RequestParam(defaultValue="50")@Min(1)@Max(100)int limit){return service.byHashtag(tag,limit).stream().map(PostResponse::from).toList();}
 @Operation(summary="Find public posts linked to a catalog asset")@GetMapping("/catalog/{assetId}")
 public List<PostResponse> catalog(@PathVariable UUID assetId,@RequestParam(defaultValue="50")@Min(1)@Max(100)int limit){return service.byCatalogAsset(assetId,limit).stream().map(PostResponse::from).toList();}
 private PostCommand command(PostRequest r){PostReferenceType type=r.referenceType()!=null?r.referenceType():r.catalogAssetId()!=null?PostReferenceType.PLACE:PostReferenceType.NONE;String id=r.referenceId()!=null?r.referenceId():r.catalogAssetId()!=null?r.catalogAssetId().toString():null;return new PostCommand(r.caption(),r.visibility(),r.catalogAssetId(),r.mediaIds(),r.hashtags(),type,id,r.countryCode(),r.adminLevel1Id(),r.adminLevel2Id(),r.cityId(),r.localityId(),r.latitude(),r.longitude(),r.languageCode());}
 private boolean admin(Authentication a){return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(v->v.equals("ROLE_ADMIN")||v.equals("ROLE_SUPER_ADMIN")||v.equals("ROLE_MODERATOR"));}
}
