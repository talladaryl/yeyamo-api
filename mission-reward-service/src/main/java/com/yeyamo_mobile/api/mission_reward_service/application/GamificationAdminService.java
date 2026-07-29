package com.yeyamo_mobile.api.mission_reward_service.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.mission_reward_service.application.port.MissionOutboxPort;
import com.yeyamo_mobile.api.mission_reward_service.domain.AdminDefinitionStatus;
import com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence.*;

@Service
public class GamificationAdminService {
    private final BadgeDefinitionRepository badges;private final XpRuleRepository rules;private final RewardDefinitionRepository rewards;private final FraudAlertRepository fraud;private final MissionOutboxPort outbox;
    public GamificationAdminService(BadgeDefinitionRepository badges,XpRuleRepository rules,RewardDefinitionRepository rewards,FraudAlertRepository fraud,MissionOutboxPort outbox){this.badges=badges;this.rules=rules;this.rewards=rewards;this.fraud=fraud;this.outbox=outbox;}
    @Transactional(readOnly=true)public Page<BadgeDefinitionEntity> badges(Pageable page){return badges.findAll(page);}
    @Transactional(readOnly=true)public BadgeDefinitionEntity badge(UUID id){return badges.findById(id).orElseThrow(()->new NoSuchElementException("Badge not found"));}
    @Transactional public BadgeDefinitionEntity createBadge(BadgeCommand c,String correlation){if(badges.existsByCode(c.code()))throw new IllegalArgumentException("Badge code already exists");var value=badges.save(BadgeDefinitionEntity.create(c.code(),c.name(),c.description(),c.condition()));emit("badge.created",value.getId(),correlation);return value;}
    @Transactional public BadgeDefinitionEntity updateBadge(UUID id,BadgeCommand c,String correlation){var value=badge(id);value.update(c.name(),c.description(),c.condition());badges.save(value);emit("badge.updated",id,correlation);return value;}
    @Transactional public BadgeDefinitionEntity badgeStatus(UUID id,AdminDefinitionStatus status,String correlation){var value=badge(id);value.status(status);badges.save(value);emit("badge.status.changed",id,correlation);return value;}
    @Transactional(readOnly=true)public Page<XpRuleEntity> rules(Pageable page){return rules.findAll(page);}
    @Transactional public XpRuleEntity createRule(XpRuleCommand c,String correlation){if(rules.existsByAction(c.action()))throw new IllegalArgumentException("XP action already exists");var value=rules.save(XpRuleEntity.create(c.action(),c.xpAmount(),c.frequencyLimit(),c.dailyLimit(),c.conditions()));emit("xp.rule.created",value.getId(),correlation);return value;}
    @Transactional public XpRuleEntity updateRule(UUID id,XpRuleCommand c,String correlation){var value=rules.findById(id).orElseThrow(()->new NoSuchElementException("XP rule not found"));value.update(c.xpAmount(),c.frequencyLimit(),c.dailyLimit(),c.conditions());rules.save(value);emit("xp.rule.updated",id,correlation);return value;}
    @Transactional public XpRuleEntity ruleStatus(UUID id,AdminDefinitionStatus status,String correlation){var value=rules.findById(id).orElseThrow(()->new NoSuchElementException("XP rule not found"));value.status(status);rules.save(value);emit("xp.rule.status.changed",id,correlation);return value;}
    @Transactional(readOnly=true)public Page<RewardDefinitionEntity> rewards(Pageable page){return rewards.findAll(page);}
    @Transactional public RewardDefinitionEntity createReward(RewardCommand c,String correlation){if(rewards.existsByCode(c.code()))throw new IllegalArgumentException("Reward code already exists");var value=rewards.save(RewardDefinitionEntity.create(c.code(),c.type(),c.value(),c.xpCost(),c.stock(),c.currency()));emit("reward.definition.created",value.getId(),correlation);return value;}
    @Transactional public RewardDefinitionEntity updateReward(UUID id,RewardCommand c,String correlation){var value=rewards.findById(id).orElseThrow(()->new NoSuchElementException("Reward not found"));value.update(c.type(),c.value(),c.xpCost(),c.stock(),c.currency());rewards.save(value);emit("reward.definition.updated",id,correlation);return value;}
    @Transactional public RewardDefinitionEntity rewardStatus(UUID id,AdminDefinitionStatus status,String correlation){var value=rewards.findById(id).orElseThrow(()->new NoSuchElementException("Reward not found"));value.status(status);rewards.save(value);emit("reward.definition.status.changed",id,correlation);return value;}
    @Transactional(readOnly=true)public Page<FraudAlertEntity> fraud(String status,String userId,String rule,String severity,Instant from,Instant to,Pageable page){
        Specification<FraudAlertEntity> spec=(root,query,builder)->builder.conjunction();
        if(has(status))spec=spec.and((r,q,b)->b.equal(r.get("status"),status));if(has(userId))spec=spec.and((r,q,b)->b.equal(r.get("userId"),userId));if(has(rule))spec=spec.and((r,q,b)->b.equal(r.get("rule"),rule));if(has(severity))spec=spec.and((r,q,b)->b.equal(r.get("severity"),severity));if(from!=null)spec=spec.and((r,q,b)->b.greaterThanOrEqualTo(r.get("createdAt"),from));if(to!=null)spec=spec.and((r,q,b)->b.lessThanOrEqualTo(r.get("createdAt"),to));return fraud.findAll(spec,page);
    }
    private void emit(String type,UUID id,String correlation){outbox.append(type,id.toString(),correlation,Map.of("id",id));}
    private static boolean has(String value){return value!=null&&!value.isBlank();}
    public record BadgeCommand(String code,String name,String description,String condition){}
    public record XpRuleCommand(String action,int xpAmount,Integer frequencyLimit,Integer dailyLimit,String conditions){}
    public record RewardCommand(String code,String type,BigDecimal value,int xpCost,Integer stock,String currency){}
}
