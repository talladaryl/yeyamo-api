package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;

import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.mission_reward_service.domain.RewardGrantStatus;import jakarta.persistence.*;
@Entity@Table(name="mission_reward_grants")
public class RewardGrantEntity{
 @Id private UUID id;@OneToOne(fetch=FetchType.EAGER,optional=false)@JoinColumn(name="user_mission_id",unique=true)private UserMissionEntity userMission;
 @Column(name="user_id",nullable=false,length=120)private String userId;@Column(name="reward_code",nullable=false,length=100)private String rewardCode;
 @Column(name="reward_title",nullable=false,length=200)private String rewardTitle;@Column(nullable=false)private int amount;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=30)private RewardGrantStatus status;@Column(name="granted_at")private Instant grantedAt;
 @Column(name="failure_reason",length=1000)private String failureReason;@Column(name="created_at",nullable=false)private Instant createdAt;
 @Column(name="updated_at",nullable=false)private Instant updatedAt;@Version private long version;
 public static RewardGrantEntity pending(UserMissionEntity um){var m=um.getMission();var e=new RewardGrantEntity();e.id=UUID.randomUUID();e.userMission=um;e.userId=um.getUserId();e.rewardCode=m.getRewardCode();e.rewardTitle=m.getRewardTitle();e.amount=m.getRewardAmount();e.status=RewardGrantStatus.PENDING;e.createdAt=Instant.now();e.updatedAt=e.createdAt;return e;}
 public void grant(){status=RewardGrantStatus.GRANTED;failureReason=null;grantedAt=Instant.now();updatedAt=grantedAt;userMission.rewarded();}
 public void fail(String reason){status=RewardGrantStatus.FAILED;failureReason=reason;updatedAt=Instant.now();}
 public UUID getId(){return id;}public String getUserId(){return userId;}public String getRewardCode(){return rewardCode;}public String getRewardTitle(){return rewardTitle;}public int getAmount(){return amount;}public RewardGrantStatus getStatus(){return status;}public Instant getGrantedAt(){return grantedAt;}public String getFailureReason(){return failureReason;}public UserMissionEntity getUserMission(){return userMission;}
}
