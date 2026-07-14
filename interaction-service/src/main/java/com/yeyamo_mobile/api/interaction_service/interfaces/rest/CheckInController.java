package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.util.*;import org.springframework.http.*;import org.springframework.security.core.Authentication;import org.springframework.validation.annotation.Validated;import org.springframework.web.bind.annotation.*;import com.yeyamo_mobile.api.interaction_service.application.*;
import io.swagger.v3.oas.annotations.*;import io.swagger.v3.oas.annotations.security.SecurityRequirement;import io.swagger.v3.oas.annotations.tags.Tag;import jakarta.validation.Valid;import jakarta.validation.constraints.*;
@RestController @RequestMapping("/api/v1/checkins")@Validated @Tag(name="Check-ins")
public class CheckInController{private final InteractionCommandService commands;private final InteractionQueryService queries;public CheckInController(InteractionCommandService c,InteractionQueryService q){commands=c;queries=q;}
 @PostMapping@ResponseStatus(HttpStatus.CREATED)@Operation(summary="Check in at a catalog asset",security=@SecurityRequirement(name="bearerAuth"))
 public CheckInResponse create(@Valid@RequestBody CheckInRequest r,@RequestHeader("Idempotency-Key")@NotBlank String key,@RequestHeader(value="X-Correlation-Id",required=false)String correlation,Authentication auth){
  return CheckInResponse.from(commands.checkIn(r.catalogAssetId(),auth.getName(),r.latitude(),r.longitude(),r.visible(),key,correlation));}
 @GetMapping("/me")@Operation(summary="List my check-ins",security=@SecurityRequirement(name="bearerAuth"))
 public List<CheckInResponse> mine(@RequestParam(defaultValue="50")@Min(1)@Max(100)int limit,Authentication auth){return queries.checkIns(auth.getName(),limit).stream().map(CheckInResponse::from).toList();}}
