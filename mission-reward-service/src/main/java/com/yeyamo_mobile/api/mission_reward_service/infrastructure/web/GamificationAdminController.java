package com.yeyamo_mobile.api.mission_reward_service.infrastructure.web;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.mission_reward_service.application.GamificationAdminService;
import com.yeyamo_mobile.api.mission_reward_service.application.GamificationAdminService.*;
import com.yeyamo_mobile.api.mission_reward_service.domain.AdminDefinitionStatus;
import com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/mission-management")
public class GamificationAdminController {
    private final GamificationAdminService service;public GamificationAdminController(GamificationAdminService service){this.service=service;}
    @GetMapping("/badges")public Page<BadgeDefinitionEntity> badges(Pageable page){return service.badges(page);}
    @GetMapping("/badges/{id}")public BadgeDefinitionEntity badge(@PathVariable UUID id){return service.badge(id);}
    @PostMapping("/badges")public BadgeDefinitionEntity createBadge(@Valid@RequestBody BadgeCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.createBadge(body,correlation);}
    @PutMapping("/badges/{id}")public BadgeDefinitionEntity updateBadge(@PathVariable UUID id,@Valid@RequestBody BadgeCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.updateBadge(id,body,correlation);}
    @PatchMapping("/badges/{id}/status")public BadgeDefinitionEntity badgeStatus(@PathVariable UUID id,@RequestBody StatusCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.badgeStatus(id,body.status(),correlation);}
    @GetMapping("/xp-rules")public Page<XpRuleEntity> rules(Pageable page){return service.rules(page);}
    @PostMapping("/xp-rules")public XpRuleEntity createRule(@RequestBody XpRuleCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.createRule(body,correlation);}
    @PutMapping("/xp-rules/{id}")public XpRuleEntity updateRule(@PathVariable UUID id,@RequestBody XpRuleCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.updateRule(id,body,correlation);}
    @PatchMapping("/xp-rules/{id}/status")public XpRuleEntity ruleStatus(@PathVariable UUID id,@RequestBody StatusCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.ruleStatus(id,body.status(),correlation);}
    @GetMapping("/rewards")public Page<RewardDefinitionEntity> rewards(Pageable page){return service.rewards(page);}
    @PostMapping("/rewards")public RewardDefinitionEntity createReward(@RequestBody RewardCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.createReward(body,correlation);}
    @PutMapping("/rewards/{id}")public RewardDefinitionEntity updateReward(@PathVariable UUID id,@RequestBody RewardCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.updateReward(id,body,correlation);}
    @PatchMapping("/rewards/{id}/status")public RewardDefinitionEntity rewardStatus(@PathVariable UUID id,@RequestBody StatusCommand body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.rewardStatus(id,body.status(),correlation);}
    @GetMapping("/fraud-alerts")public Page<FraudAlertEntity> fraud(@RequestParam(required=false)String status,@RequestParam(required=false)String userId,@RequestParam(required=false)String rule,@RequestParam(required=false)String severity,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdFrom,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdTo,Pageable page){return service.fraud(status,userId,rule,severity,createdFrom,createdTo,page);}
    public record StatusCommand(AdminDefinitionStatus status){}
}
