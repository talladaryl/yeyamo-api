package com.yeyamo_mobile.api.interaction_service.domain.model;
import java.time.Instant;import java.util.UUID;
public class Comment{
 private UUID id;private UUID postId;private UUID parentId;private String authorId;private String body;private CommentStatus status;private Instant createdAt;private Instant updatedAt;private Instant deletedAt;private long version;
 public static Comment create(UUID postId,UUID parentId,String authorId,String body){String text=normalize(body);if(postId==null)throw new IllegalArgumentException("postId is required");if(authorId==null||authorId.isBlank())throw new IllegalArgumentException("authorId is required");
  Comment c=new Comment();c.id=UUID.randomUUID();c.postId=postId;c.parentId=parentId;c.authorId=authorId;c.body=text;c.status=CommentStatus.ACTIVE;c.createdAt=Instant.now();c.updatedAt=c.createdAt;return c;}
 public void update(String body){if(status!=CommentStatus.ACTIVE)throw new IllegalStateException("Deleted comment cannot be edited");this.body=normalize(body);updatedAt=Instant.now();}
 public void delete(){if(status==CommentStatus.DELETED)return;status=CommentStatus.DELETED;body=null;deletedAt=Instant.now();updatedAt=deletedAt;}
 private static String normalize(String body){if(body==null||body.isBlank())throw new IllegalArgumentException("comment body is required");String v=body.trim();if(v.length()>2000)throw new IllegalArgumentException("comment exceeds 2000 characters");return v;}
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public UUID getPostId(){return postId;}public void setPostId(UUID v){postId=v;}public UUID getParentId(){return parentId;}public void setParentId(UUID v){parentId=v;}
 public String getAuthorId(){return authorId;}public void setAuthorId(String v){authorId=v;}public String getBody(){return body;}public void setBody(String v){body=v;}public CommentStatus getStatus(){return status;}public void setStatus(CommentStatus v){status=v;}
 public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}public Instant getUpdatedAt(){return updatedAt;}public void setUpdatedAt(Instant v){updatedAt=v;}public Instant getDeletedAt(){return deletedAt;}public void setDeletedAt(Instant v){deletedAt=v;}
 public long getVersion(){return version;}public void setVersion(long v){version=v;}
}
