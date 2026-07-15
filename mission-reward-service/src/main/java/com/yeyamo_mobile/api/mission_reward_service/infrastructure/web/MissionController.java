package com.yeyamo_mobile.api.mission_reward_service.infrastructure.web;
import java.util.*;import org.springframework.http.HttpStatus;import org.springframework.security.core.Authentication;import org.springframework.validation.annotation.Validated;import org.springframework.web.bind.annotation.*;import com.yeyamo_mobile.api.mission_reward_service.application.*;import com.yeyamo_mobile.api.mission_reward_service.application.MissionDtos.*;import io.swagger.v3.oas.annotations.Operation;import io.swagger.v3.oas.annotations.security.SecurityRequirement;import jakarta.validation.Valid;
@RestController@Validated@SecurityRequirement(name="bearerAuth")
public class MissionController{
 private final MissionApplicationService service;public MissionController(MissionApplicationService s){service=s;}
 @GetMapping("/api/v1/missions")@Operation(summary="List mission definitions")public List<MissionView>catalog(){return service.catalog();}
 @GetMapping("/api/v1/me/missions")@Operation(summary="Read my mission progression")public List<MissionView>mine(Authentication auth){return service.mine(auth.getName());}
 @GetMapping("/api/v1/me/mission-rewards")@Operation(summary="Read my mission rewards")public List<RewardView>rewards(Authentication auth){return service.rewards(auth.getName());}
 @PostMapping("/api/v1/mission-management/missions")@ResponseStatus(HttpStatus.CREATED)@Operation(summary="Create a mission")public MissionView create(@Valid@RequestBody CreateMission body,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.create(body,correlation);}
 @PostMapping("/api/v1/mission-management/missions/{id}/activate")public MissionView activate(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.activate(id,correlation);}
 @PostMapping("/api/v1/mission-management/missions/{id}/pause")public MissionView pause(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return service.pause(id,correlation);}
}
