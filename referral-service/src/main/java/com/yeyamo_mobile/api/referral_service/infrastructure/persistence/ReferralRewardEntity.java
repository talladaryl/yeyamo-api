package com.yeyamo_mobile.api.referral_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.referral_service.domain.*;import jakarta.persistence.*;
@Entity@Table(name="referral_rewards")public class ReferralRewardEntity{
 @Id private UUID id;@ManyToOne(fetch=FetchType.EAGER,optional=false)@JoinColumn(name="attribution_id")private ReferralAttributionEntity attribution;
 @Column(name="beneficiary_user_id",nullable=false,length=120)private String beneficiaryUserId;@Enumerated(EnumType.STRING)@Column(name="beneficiary_type",nullable=false,length=20)private BeneficiaryType beneficiaryType;
 @Column(name="reward_code",nullable=false,length=100)private String rewardCode;@Column(nullable=false,length=200)private String title;@Column(nullable=false)private int amount;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=20)private RewardStatus status;@Column(name="created_at",nullable=false)private Instant createdAt;
 public static ReferralRewardEntity grant(ReferralAttributionEntity a,BeneficiaryType type,String code,String title,int amount){var e=new ReferralRewardEntity();e.id=UUID.randomUUID();e.attribution=a;e.beneficiaryType=type;e.beneficiaryUserId=type==BeneficiaryType.REFERRER?a.getReferrerUserId():a.getReferredUserId();e.rewardCode=code;e.title=title;e.amount=amount;e.status=RewardStatus.GRANTED;e.createdAt=Instant.now();return e;}
 public UUID getId(){return id;}public ReferralAttributionEntity getAttribution(){return attribution;}public String getBeneficiaryUserId(){return beneficiaryUserId;}public BeneficiaryType getBeneficiaryType(){return beneficiaryType;}public String getRewardCode(){return rewardCode;}public String getTitle(){return title;}public int getAmount(){return amount;}public RewardStatus getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}
}
