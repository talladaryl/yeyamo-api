package com.yeyamo_mobile.api.culture_service.domain;

import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.RecipeRevisionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="culture_recipe_revisions")
public class CultureRecipeRevision {
 @Id private UUID id;
 @Column(name="content_id",nullable=false) private UUID contentId;
 @Column(name="author_id",nullable=false) private String authorId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private RecipeRevisionStatus status;
 @Column(name="base_version",nullable=false) private long baseVersion;
 @Column(name="candidate_json",nullable=false,columnDefinition="TEXT") private String candidateJson;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @Column(name="submitted_at") private Instant submittedAt;
 @Column(name="decided_at") private Instant decidedAt;
 @Column(name="reviewed_by") private String reviewedBy;
 @Column(name="review_reason",columnDefinition="TEXT") private String reviewReason;
 protected CultureRecipeRevision(){}
 public static CultureRecipeRevision draft(UUID contentId,String author,long baseVersion,String json){CultureRecipeRevision r=new CultureRecipeRevision();r.id=UUID.randomUUID();r.contentId=contentId;r.authorId=author;r.status=RecipeRevisionStatus.DRAFT;r.baseVersion=baseVersion;r.candidateJson=json;r.createdAt=Instant.now();r.updatedAt=r.createdAt;return r;}
 public void replaceDraft(long baseVersion,String json,String actor){owner(actor);if(status!=RecipeRevisionStatus.DRAFT&&status!=RecipeRevisionStatus.REJECTED)throw new IllegalStateException("Only a draft or rejected revision can be edited");this.baseVersion=baseVersion;candidateJson=json;status=RecipeRevisionStatus.DRAFT;submittedAt=null;decidedAt=null;reviewedBy=null;reviewReason=null;updatedAt=Instant.now();}
 public void submit(String actor){owner(actor);if(status!=RecipeRevisionStatus.DRAFT)throw new IllegalStateException("Only a draft revision can be submitted");status=RecipeRevisionStatus.SUBMITTED;submittedAt=Instant.now();updatedAt=submittedAt;}
 public void beginReview(){if(status!=RecipeRevisionStatus.SUBMITTED)throw new IllegalStateException("Only a submitted revision can be reviewed");status=RecipeRevisionStatus.UNDER_REVIEW;updatedAt=Instant.now();}
 public void decide(boolean approved,String reviewer,String reason){if(status!=RecipeRevisionStatus.UNDER_REVIEW)throw new IllegalStateException("Revision is not under review");status=approved?RecipeRevisionStatus.APPROVED:RecipeRevisionStatus.REJECTED;reviewedBy=reviewer;reviewReason=reason;decidedAt=Instant.now();updatedAt=decidedAt;}
 private void owner(String actor){if(!authorId.equals(actor))throw new SecurityException("Revision owner mismatch");}
 public UUID getId(){return id;}public UUID getContentId(){return contentId;}public String getAuthorId(){return authorId;}public RecipeRevisionStatus getStatus(){return status;}public long getBaseVersion(){return baseVersion;}public String getCandidateJson(){return candidateJson;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}public Instant getSubmittedAt(){return submittedAt;}public Instant getDecidedAt(){return decidedAt;}public String getReviewedBy(){return reviewedBy;}public String getReviewReason(){return reviewReason;}
}
