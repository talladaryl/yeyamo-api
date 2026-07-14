package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="interaction_shares")
public class ShareEntity{@Id private UUID id;@Column(name="post_id",nullable=false)private UUID postId;@Column(name="user_id",nullable=false,length=100)private String userId;@Column(nullable=false,length=40)private String channel;@Column(name="created_at",nullable=false)private Instant createdAt;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public UUID getPostId(){return postId;}public void setPostId(UUID v){postId=v;}public String getUserId(){return userId;}public void setUserId(String v){userId=v;}public String getChannel(){return channel;}public void setChannel(String v){channel=v;}public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}}
