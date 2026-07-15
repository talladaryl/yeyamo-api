package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;

import java.time.Instant;import java.util.UUID;
import com.yeyamo_mobile.api.mission_reward_service.domain.UserMissionStatus;
import jakarta.persistence.*;

@Entity @Table(name="user_missions")
public class UserMissionEntity {
 @Id private UUID id; @Column(name="user_id",nullable=false,length=120) private String userId;
 @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="mission_id") private MissionDefinitionEntity mission;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private UserMissionStatus status;
 @Column(name="enrolled_at",nullable=false) private Instant enrolledAt; @Column(name="completed_at") private Instant completedAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt; @Version private long version;
 public static UserMissionEntity enroll(String user,MissionDefinitionEntity mission){var e=new UserMissionEntity();e.id=UUID.randomUUID();e.userId=user;e.mission=mission;e.status=UserMissionStatus.ACTIVE;e.enrolledAt=Instant.now();e.updatedAt=e.enrolledAt;return e;}
 public void complete(){if(status==UserMissionStatus.ACTIVE){status=UserMissionStatus.COMPLETED;completedAt=Instant.now();updatedAt=completedAt;}}
 public void rewarded(){status=UserMissionStatus.REWARDED;updatedAt=Instant.now();}
 public UUID getId(){return id;}public String getUserId(){return userId;}public MissionDefinitionEntity getMission(){return mission;}public UserMissionStatus getStatus(){return status;}public Instant getEnrolledAt(){return enrolledAt;}public Instant getCompletedAt(){return completedAt;}
}
