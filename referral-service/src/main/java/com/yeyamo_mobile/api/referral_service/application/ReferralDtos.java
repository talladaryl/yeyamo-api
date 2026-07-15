package com.yeyamo_mobile.api.referral_service.application;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.referral_service.domain.*;import jakarta.validation.constraints.*;
public final class ReferralDtos{private ReferralDtos(){}
 public record CreateCode(@Min(1)@Max(1000)int maxUses,Instant expiresAt){}
 public record Invite(@NotBlank String code,@Email@NotBlank String email){}
 public record Redeem(@NotBlank String code){}
 public record CodeView(UUID id,String code,ReferralCodeStatus status,int maxUses,int usageCount,Instant expiresAt,Instant createdAt){}
 public record InvitationView(UUID id,String code,InvitationStatus status,String invitedUserId,Instant createdAt){}
 public record AttributionView(UUID id,String code,String referrerUserId,String referredUserId,AttributionSource source,AttributionStatus status,Instant attributedAt,Instant qualifiedAt,Instant rewardedAt){}
 public record RewardView(UUID id,String code,String title,int amount,BeneficiaryType beneficiaryType,RewardStatus status,Instant createdAt){}
 public record HistoryView(UUID id,UUID attributionId,String action,String details,Instant occurredAt){}
}
