package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;import com.yeyamo_mobile.api.interaction_service.domain.model.CommentStatus;
@Entity @Table(name="interaction_comments")
public class CommentEntity{@Id private UUID id;@Column(name="post_id",nullable=false)private UUID postId;@Column(name="parent_id")private UUID parentId;@Column(name="author_id",nullable=false,length=100)private String authorId;
 @Column(columnDefinition="TEXT")private String body;@Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private CommentStatus status;@Column(name="created_at",nullable=false)private Instant createdAt;
 @Column(name="updated_at",nullable=false)private Instant updatedAt;@Column(name="deleted_at")private Instant deletedAt;@Version private long version;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public UUID getPostId(){return postId;}public void setPostId(UUID v){postId=v;}public UUID getParentId(){return parentId;}public void setParentId(UUID v){parentId=v;}
 public String getAuthorId(){return authorId;}public void setAuthorId(String v){authorId=v;}public String getBody(){return body;}public void setBody(String v){body=v;}public CommentStatus getStatus(){return status;}public void setStatus(CommentStatus v){status=v;}
 public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}public Instant getUpdatedAt(){return updatedAt;}public void setUpdatedAt(Instant v){updatedAt=v;}public Instant getDeletedAt(){return deletedAt;}public void setDeletedAt(Instant v){deletedAt=v;}
 public long getVersion(){return version;}public void setVersion(long v){version=v;}}
