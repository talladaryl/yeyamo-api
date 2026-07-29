package com.yeyamo_mobile.api.notification_service.newsletter;
import java.time.Instant;import java.util.*;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
public final class NewsletterDtos {
 private NewsletterDtos(){}
 public record Segment(@Size(max=20)Set<@Pattern(regexp="[A-Z_]{2,40}")String> userTypes,@Size(max=20)Set<@Pattern(regexp="[A-Za-z0-9_-]{1,80}")String> regionIds,@Size(max=20)Set<@Pattern(regexp="[A-Z_]{2,40}")String> activityLevels,@Size(max=20)Set<@Pattern(regexp="[A-Z_]{2,40}")String> partnerStatuses){}
 public record CampaignRequest(@NotBlank@Size(max=200)String name,@NotBlank@Size(max=300)String subject,@Size(max=300)String preheader,@NotBlank@Size(max=100000)String content,@NotNull@Valid Segment audience){}
 public record ScheduleRequest(@NotNull@Future Instant scheduledAt){}
 public record CampaignResponse(UUID id,String name,String subject,String preheader,String content,NewsletterStatus status,Segment audience,Instant scheduledAt,String createdBy,Instant createdAt,Instant updatedAt){}
 public record StatsResponse(UUID campaignId,long delivered,long failed,long opened,long clicked,long unsubscribed,Instant updatedAt){}
}
