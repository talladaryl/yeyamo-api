package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;

import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="mission_progress")
public class MissionProgressEntity {
 @Id private UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="user_mission_id")private UserMissionEntity userMission;
 @ManyToOne(fetch=FetchType.EAGER,optional=false)@JoinColumn(name="objective_id")private MissionObjectiveEntity objective;
 @Column(name="current_value",nullable=false)private long currentValue;@Column(name="completed_at")private Instant completedAt;
 @Column(name="updated_at",nullable=false)private Instant updatedAt;@Version private long version;
 public static MissionProgressEntity start(UserMissionEntity userMission,MissionObjectiveEntity objective){var e=new MissionProgressEntity();e.id=UUID.randomUUID();e.userMission=userMission;e.objective=objective;e.updatedAt=Instant.now();return e;}
 public void advance(long value){currentValue=value;if(completedAt==null&&value>=objective.getTargetValue())completedAt=Instant.now();updatedAt=Instant.now();}
 public boolean complete(){return currentValue>=objective.getTargetValue();}public UUID getId(){return id;}public long getCurrentValue(){return currentValue;}public Instant getCompletedAt(){return completedAt;}public MissionObjectiveEntity getObjective(){return objective;}public UserMissionEntity getUserMission(){return userMission;}
}
