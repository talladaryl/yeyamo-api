package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.mission_reward_service.domain.MissionStatus;
import jakarta.persistence.*;

@Entity @Table(name = "mission_definitions")
public class MissionDefinitionEntity {
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=80) private String code;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false, length=1000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private MissionStatus status;
    @Column(name="starts_at") private Instant startsAt;
    @Column(name="ends_at") private Instant endsAt;
    @Column(name="reward_code", nullable=false, length=100) private String rewardCode;
    @Column(name="reward_title", nullable=false, length=200) private String rewardTitle;
    @Column(name="reward_amount", nullable=false) private int rewardAmount;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Version private long version;

    public static MissionDefinitionEntity draft(UUID id, String code, String title, String description,
            Instant startsAt, Instant endsAt, String rewardCode, String rewardTitle, int rewardAmount) {
        if (code == null || code.isBlank() || title == null || title.isBlank() || rewardAmount <= 0)
            throw new IllegalArgumentException("Invalid mission definition");
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt))
            throw new IllegalArgumentException("endsAt must be after startsAt");
        var entity = new MissionDefinitionEntity();
        entity.id=id; entity.code=code; entity.title=title; entity.description=description;
        entity.status=MissionStatus.DRAFT; entity.startsAt=startsAt; entity.endsAt=endsAt;
        entity.rewardCode=rewardCode; entity.rewardTitle=rewardTitle; entity.rewardAmount=rewardAmount;
        entity.createdAt=Instant.now(); entity.updatedAt=entity.createdAt; return entity;
    }
    public void activate(){status=MissionStatus.ACTIVE;updatedAt=Instant.now();}
    public void pause(){status=MissionStatus.PAUSED;updatedAt=Instant.now();}
    public void update(String title,String description,Instant startsAt,Instant endsAt,String rewardCode,String rewardTitle,int rewardAmount){
        if(title==null||title.isBlank()||description==null||description.isBlank()||rewardCode==null||rewardCode.isBlank()||rewardTitle==null||rewardTitle.isBlank()||rewardAmount<=0)throw new IllegalArgumentException("Invalid mission definition");
        if(startsAt!=null&&endsAt!=null&&!endsAt.isAfter(startsAt))throw new IllegalArgumentException("endsAt must be after startsAt");
        this.title=title.trim();this.description=description.trim();this.startsAt=startsAt;this.endsAt=endsAt;this.rewardCode=rewardCode.trim();this.rewardTitle=rewardTitle.trim();this.rewardAmount=rewardAmount;updatedAt=Instant.now();
    }
    public void archive(){status=MissionStatus.ARCHIVED;updatedAt=Instant.now();}
    public boolean accepts(Instant at){return status==MissionStatus.ACTIVE&&(startsAt==null||!at.isBefore(startsAt))&&(endsAt==null||at.isBefore(endsAt));}
    public UUID getId(){return id;} public String getCode(){return code;} public String getTitle(){return title;}
    public String getDescription(){return description;} public MissionStatus getStatus(){return status;}
    public Instant getStartsAt(){return startsAt;} public Instant getEndsAt(){return endsAt;}
    public String getRewardCode(){return rewardCode;} public String getRewardTitle(){return rewardTitle;}
    public int getRewardAmount(){return rewardAmount;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
