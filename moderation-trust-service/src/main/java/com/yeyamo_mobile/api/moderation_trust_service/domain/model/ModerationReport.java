package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

import java.time.Instant;import java.util.UUID;

public class ModerationReport {
 private UUID id;
 private TargetType targetType;
 private String targetId;
 private String targetOwnerId;
 private String reporterId;
 private ReportReason reason;
 private String details;
 private ReportStatus status;
 private String assignedTo;
 private String resolution;
 private Instant createdAt;
 private Instant updatedAt;
 private Instant decidedAt;
 
 private long version;
 public static ModerationReport create(TargetType type,String targetId,String owner,String reporter,ReportReason reason,String details){
  if(type==null)throw new IllegalArgumentException("targetType is required");if(blank(targetId))throw new IllegalArgumentException("targetId is required");if(blank(reporter))throw new IllegalArgumentException("reporterId is required");if(reason==null)throw new IllegalArgumentException("reason is required");
  ModerationReport r=new ModerationReport();r.id=UUID.randomUUID();r.targetType=type;r.targetId=targetId.trim();r.targetOwnerId=norm(owner);r.reporterId=reporter.trim();r.reason=reason;r.details=limit(details,2000);r.status=ReportStatus.OPEN;r.createdAt=Instant.now();r.updatedAt=r.createdAt;return r;}
 public void startReview(String moderator){if(status!=ReportStatus.OPEN)throw new IllegalStateException("Only an OPEN report can enter REVIEW");if(blank(moderator))throw new IllegalArgumentException("moderator is required");status=ReportStatus.REVIEW;assignedTo=moderator.trim();updatedAt=Instant.now();}
 public void decide(ReportStatus decision,String moderator,String resolution){if(status!=ReportStatus.REVIEW)throw new IllegalStateException("Only a report in REVIEW can be decided");if(decision!=ReportStatus.APPROVED&&decision!=ReportStatus.REJECTED)throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");if(blank(moderator))throw new IllegalArgumentException("moderator is required");if(blank(resolution))throw new IllegalArgumentException("resolution is required");status=decision;assignedTo=moderator.trim();this.resolution=limit(resolution,2000);decidedAt=Instant.now();updatedAt=decidedAt;}
 private static String limit(String v,int max){String n=norm(v);if(n!=null&&n.length()>max)throw new IllegalArgumentException("text exceeds "+max+" characters");return n;}private static String norm(String v){return blank(v)?null:v.trim();}private static boolean blank(String v){return v==null||v.isBlank();}
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public TargetType getTargetType(){return targetType;}public void setTargetType(TargetType v){targetType=v;}public String getTargetId(){return targetId;}public void setTargetId(String v){targetId=v;}public String getTargetOwnerId(){return targetOwnerId;}public void setTargetOwnerId(String v){targetOwnerId=v;}public String getReporterId(){return reporterId;}public void setReporterId(String v){reporterId=v;}public ReportReason getReason(){return reason;}public void setReason(ReportReason v){reason=v;}public String getDetails(){return details;}public void setDetails(String v){details=v;}public ReportStatus getStatus(){return status;}public void setStatus(ReportStatus v){status=v;}public String getAssignedTo(){return assignedTo;}public void setAssignedTo(String v){assignedTo=v;}public String getResolution(){return resolution;}public void setResolution(String v){resolution=v;}public Instant getCreatedAt(){return createdAt;}public void setCreatedAt(Instant v){createdAt=v;}public Instant getUpdatedAt(){return updatedAt;}public void setUpdatedAt(Instant v){updatedAt=v;}public Instant getDecidedAt(){return decidedAt;}public void setDecidedAt(Instant v){decidedAt=v;}public long getVersion(){return version;}public void setVersion(long v){version=v;}
}
