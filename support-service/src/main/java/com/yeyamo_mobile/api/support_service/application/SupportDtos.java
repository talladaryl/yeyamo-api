package com.yeyamo_mobile.api.support_service.application;
import java.time.*;import java.util.*;import com.yeyamo_mobile.api.support_service.domain.*;import jakarta.validation.constraints.*;
public final class SupportDtos {
 private SupportDtos(){}
 public record MessageRequest(@NotBlank @Size(max=10000)String content,@Size(max=10)List<@Pattern(regexp="[A-Za-z0-9_-]{1,160}") String> attachmentMediaIds){}
 public record NoteRequest(@NotBlank @Size(max=10000)String content){}
 public record AssignRequest(@NotBlank @Size(max=120)String assigneeId){}
 public record StatusRequest(@NotNull SupportStatus status,@Size(max=1000)String reason){}
 public record PriorityRequest(@NotNull SupportPriority priority,@Size(max=1000)String reason){}
 public record ConversationSummary(UUID id,String userId,String subject,SupportStatus status,SupportPriority priority,String assigneeAdminId,Instant createdAt,Instant updatedAt,Instant firstResponseAt,Instant resolvedAt,Long responseDurationSeconds,Long resolutionDurationSeconds){}
 public record MessageResponse(UUID id,String senderType,String senderId,String content,List<String> attachmentMediaIds,Instant createdAt){}
 public record NoteResponse(UUID id,String authorAdminId,String content,Instant createdAt){}
 public record ConversationDetail(ConversationSummary conversation,List<MessageResponse> messages,List<NoteResponse> internalNotes){}
}
