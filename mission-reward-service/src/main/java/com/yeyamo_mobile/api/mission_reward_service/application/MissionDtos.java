package com.yeyamo_mobile.api.mission_reward_service.application;

import java.time.Instant;import java.util.*;
import com.yeyamo_mobile.api.mission_reward_service.domain.*;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;

public final class MissionDtos {
 private MissionDtos(){}
 public record CreateMission(@NotBlank String code,@NotBlank String title,@NotBlank String description,
   Instant startsAt,Instant endsAt,@NotBlank String rewardCode,@NotBlank String rewardTitle,@Positive int rewardAmount,
   @NotEmpty List<@Valid ObjectiveInput> objectives){}
 public record ObjectiveInput(@NotBlank String label,@NotBlank String eventType,@NotNull ObjectiveMetric metric,
   @Positive long targetValue,String valueField,String ruleKey,String ruleValue){}
 public record ObjectiveView(UUID id,String label,String eventType,ObjectiveMetric metric,long target,long current,boolean completed,String ruleKey,String ruleValue){}
 public record MissionView(UUID id,String code,String title,String description,MissionStatus status,Instant startsAt,Instant endsAt,
   String rewardCode,String rewardTitle,int rewardAmount,List<ObjectiveView>objectives,UserMissionStatus userStatus,Instant completedAt){}
 public record RewardView(UUID id,String missionCode,String code,String title,int amount,RewardGrantStatus status,Instant grantedAt,String failureReason){}
}
