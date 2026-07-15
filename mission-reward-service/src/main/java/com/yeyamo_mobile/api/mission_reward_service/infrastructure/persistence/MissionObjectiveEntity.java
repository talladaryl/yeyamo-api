package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;

import java.util.UUID;
import com.yeyamo_mobile.api.mission_reward_service.domain.*;
import jakarta.persistence.*;

@Entity @Table(name="mission_objectives")
public class MissionObjectiveEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="mission_id") private MissionDefinitionEntity mission;
    @Column(nullable=false,length=200) private String label;
    @Column(name="event_type",nullable=false,length=140) private String eventType;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ObjectiveMetric metric;
    @Column(name="target_value",nullable=false) private long targetValue;
    @Column(name="value_field",length=100) private String valueField;
    @Column(name="rule_key",length=100) private String ruleKey;
    @Column(name="rule_value",length=200) private String ruleValue;
    @Column(nullable=false) private int position;
    public static MissionObjectiveEntity create(MissionDefinitionEntity mission,String label,String eventType,ObjectiveRule rule,int position){
        var e=new MissionObjectiveEntity();e.id=UUID.randomUUID();e.mission=mission;e.label=label;e.eventType=eventType;
        e.metric=rule.metric();e.targetValue=rule.targetValue();e.valueField=rule.valueField();e.ruleKey=rule.ruleKey();e.ruleValue=rule.ruleValue();e.position=position;return e;}
    public ObjectiveRule rule(){return new ObjectiveRule(metric,targetValue,valueField,ruleKey,ruleValue);}
    public UUID getId(){return id;} public MissionDefinitionEntity getMission(){return mission;} public String getLabel(){return label;}
    public String getEventType(){return eventType;} public ObjectiveMetric getMetric(){return metric;} public long getTargetValue(){return targetValue;}
    public String getValueField(){return valueField;} public String getRuleKey(){return ruleKey;} public String getRuleValue(){return ruleValue;} public int getPosition(){return position;}
}
