package com.yeyamo_mobile.api.mission_reward_service.application;

import java.time.Instant;import java.util.*;import java.util.stream.Collectors;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.mission_reward_service.application.MissionDtos.*;
import com.yeyamo_mobile.api.mission_reward_service.application.port.MissionOutboxPort;
import com.yeyamo_mobile.api.mission_reward_service.domain.*;
import com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence.*;

@Service
public class MissionApplicationService {
 private final MissionDefinitionRepository missions;private final MissionObjectiveRepository objectives;
 private final UserMissionRepository userMissions;private final MissionProgressRepository progress;
 private final RewardGrantRepository rewards;private final MissionOutboxPort outbox;
 public MissionApplicationService(MissionDefinitionRepository m,MissionObjectiveRepository o,UserMissionRepository u,
   MissionProgressRepository p,RewardGrantRepository r,MissionOutboxPort b){missions=m;objectives=o;userMissions=u;progress=p;rewards=r;outbox=b;}

 @Transactional public MissionView create(CreateMission command,String correlation){
  if(missions.existsByCode(command.code()))throw new IllegalArgumentException("Mission code already exists");
  var mission=missions.save(MissionDefinitionEntity.draft(UUID.randomUUID(),command.code(),command.title(),command.description(),command.startsAt(),command.endsAt(),command.rewardCode(),command.rewardTitle(),command.rewardAmount()));
  int position=0;for(var input:command.objectives()){var rule=new ObjectiveRule(input.metric(),input.targetValue(),input.valueField(),input.ruleKey(),input.ruleValue());objectives.save(MissionObjectiveEntity.create(mission,input.label(),input.eventType(),rule,position++));}
  outbox.append("mission.created",mission.getId().toString(),correlation,Map.of("missionId",mission.getId(),"code",mission.getCode()));return view(mission,null);
 }
 @Transactional public MissionView activate(UUID id,String correlation){var mission=required(id);if(objectives.findByMissionIdOrderByPositionAsc(id).isEmpty())throw new IllegalStateException("Mission requires at least one objective");mission.activate();
  outbox.append("mission.activated",id.toString(),correlation,Map.of("missionId",id,"code",mission.getCode()));return view(mission,null);}
 @Transactional public MissionView pause(UUID id,String correlation){var mission=required(id);mission.pause();outbox.append("mission.paused",id.toString(),correlation,Map.of("missionId",id,"code",mission.getCode()));return view(mission,null);}
 @Transactional public void apply(MissionEvent event){
  Map<UUID,List<MissionObjectiveEntity>> grouped=objectives.findByEventTypeOrderByPositionAsc(event.eventType()).stream()
    .filter(o->o.getMission().accepts(event.occurredAt())).filter(o->ProgressCalculator.matches(o.rule(),event))
    .collect(Collectors.groupingBy(o->o.getMission().getId(),LinkedHashMap::new,Collectors.toList()));
  for(var entry:grouped.entrySet())applyToMission(event,entry.getValue());
 }
 private void applyToMission(MissionEvent event,List<MissionObjectiveEntity> matched){
  var mission=matched.getFirst().getMission();var um=userMissions.findByUserIdAndMissionId(event.userId(),mission.getId()).orElseGet(()->userMissions.save(UserMissionEntity.enroll(event.userId(),mission)));
  if(um.getStatus()!=UserMissionStatus.ACTIVE)return;
  for(var objective:matched){var state=progress.findByUserMissionIdAndObjectiveId(um.getId(),objective.getId()).orElseGet(()->progress.save(MissionProgressEntity.start(um,objective)));
   state.advance(ProgressCalculator.next(objective.rule(),state.getCurrentValue(),event));progress.save(state);}
  var allObjectives=objectives.findByMissionIdOrderByPositionAsc(mission.getId());var states=progress.findByUserMissionId(um.getId());
  Set<UUID>completed=states.stream().filter(MissionProgressEntity::complete).map(s->s.getObjective().getId()).collect(Collectors.toSet());
  if(!allObjectives.isEmpty()&&allObjectives.stream().allMatch(o->completed.contains(o.getId())))complete(um,event.correlationId());
 }
 private void complete(UserMissionEntity um,String correlation){um.complete();userMissions.save(um);var grant=rewards.findByUserMissionId(um.getId()).orElseGet(()->rewards.save(RewardGrantEntity.pending(um)));
  outbox.append("mission.completed",um.getId().toString(),correlation,Map.of("missionId",um.getMission().getId(),"userMissionId",um.getId(),"userId",um.getUserId()));
  outbox.append("mission.reward.granted",grant.getId().toString(),correlation,Map.of("grantId",grant.getId(),"missionId",um.getMission().getId(),"userId",um.getUserId(),"rewardCode",grant.getRewardCode(),"rewardTitle",grant.getRewardTitle(),"amount",grant.getAmount()));
 }
 @Transactional(readOnly=true)public List<MissionView>catalog(){return missions.findAllByOrderByCreatedAtDesc().stream().map(m->view(m,null)).toList();}
 @Transactional(readOnly=true)public List<MissionView>mine(String user){return userMissions.findByUserIdOrderByUpdatedAtDesc(user).stream().map(um->view(um.getMission(),um)).toList();}
 @Transactional(readOnly=true)public List<RewardView>rewards(String user){return rewards.findByUserIdOrderByCreatedAtDesc(user).stream().map(this::rewardView).toList();}
 @Transactional public void confirmGrant(UUID id){rewards.findById(id).filter(r->r.getStatus()!=RewardGrantStatus.GRANTED).ifPresent(r->{r.grant();rewards.save(r);userMissions.save(r.getUserMission());});}
 @Transactional public void failGrant(UUID id,String reason){rewards.findById(id).filter(r->r.getStatus()==RewardGrantStatus.PENDING).ifPresent(r->{r.fail(reason);rewards.save(r);});}
 private MissionDefinitionEntity required(UUID id){return missions.findById(id).orElseThrow(()->new NoSuchElementException("Mission not found"));}
 private MissionView view(MissionDefinitionEntity m,UserMissionEntity um){Map<UUID,MissionProgressEntity>values=um==null?Map.of():progress.findByUserMissionId(um.getId()).stream().collect(Collectors.toMap(s->s.getObjective().getId(),s->s));
  var os=objectives.findByMissionIdOrderByPositionAsc(m.getId()).stream().map(o->{var p=values.get(o.getId());long current=p==null?0:p.getCurrentValue();return new ObjectiveView(o.getId(),o.getLabel(),o.getEventType(),o.getMetric(),o.getTargetValue(),current,current>=o.getTargetValue(),o.getRuleKey(),o.getRuleValue());}).toList();
  return new MissionView(m.getId(),m.getCode(),m.getTitle(),m.getDescription(),m.getStatus(),m.getStartsAt(),m.getEndsAt(),m.getRewardCode(),m.getRewardTitle(),m.getRewardAmount(),os,um==null?null:um.getStatus(),um==null?null:um.getCompletedAt());}
 private RewardView rewardView(RewardGrantEntity r){return new RewardView(r.getId(),r.getUserMission().getMission().getCode(),r.getRewardCode(),r.getRewardTitle(),r.getAmount(),r.getStatus(),r.getGrantedAt(),r.getFailureReason());}
}
