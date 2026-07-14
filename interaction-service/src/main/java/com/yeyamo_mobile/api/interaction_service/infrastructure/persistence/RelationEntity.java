package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;import com.yeyamo_mobile.api.interaction_service.domain.model.RelationType;
@Entity @Table(name="interaction_relations",uniqueConstraints=@UniqueConstraint(name="uk_relation_post_user_type",columnNames={"post_id","user_id","type"}))
public class RelationEntity{@Id private UUID id;@Column(name="post_id",nullable=false)private UUID postId;@Column(name="user_id",nullable=false,length=100)private String userId;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private RelationType type;@Column(name="created_at",nullable=false)private Instant createdAt;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public UUID getPostId(){return postId;}public void setPostId(UUID v){postId=v;}public String getUserId(){return userId;}public void setUserId(String v){userId=v;}
 public RelationType getType(){return type;}public void setType(RelationType v){type=v;}public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}}
