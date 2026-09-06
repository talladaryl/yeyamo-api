package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import com.yeyamo_mobile.api.interaction_service.application.GenericInteractionService;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.GenericInteractionEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interactions")
public class GenericInteractionController {
    private final GenericInteractionService service;
    public GenericInteractionController(GenericInteractionService service) { this.service = service; }
    public record Request(@Size(max = 5000) String body, @Size(max = 60) String channel) { }

    @PostMapping("/{targetType}/{targetId}/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public GenericInteractionEntity add(@PathVariable GenericInteractionEntity.TargetType targetType, @PathVariable String targetId, @PathVariable GenericInteractionEntity.Type type, @RequestBody(required = false) Request request, Authentication authentication, @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        return service.add(targetType, targetId, type, authentication.getName(), request == null ? null : request.body(), request == null ? null : request.channel(), correlation);
    }
    @DeleteMapping("/{targetType}/{targetId}/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable GenericInteractionEntity.TargetType targetType, @PathVariable String targetId, @PathVariable GenericInteractionEntity.Type type, Authentication authentication, @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        service.remove(targetType, targetId, type, authentication.getName(), correlation);
    }
    @GetMapping("/{targetType}/{targetId}/{type}/status")
    public GenericInteractionEntity status(@PathVariable GenericInteractionEntity.TargetType targetType, @PathVariable String targetId, @PathVariable GenericInteractionEntity.Type type, Authentication authentication) {
        return service.status(targetType, targetId, type, authentication.getName()).orElse(null);
    }
    @GetMapping("/{targetType}/{type}/me")
    public List<GenericInteractionEntity> mine(@PathVariable GenericInteractionEntity.TargetType targetType, @PathVariable GenericInteractionEntity.Type type, @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit, Authentication authentication) {
        return service.mine(targetType, type, authentication.getName(), limit);
    }
    @GetMapping("/{targetType}/{targetId}/comments")
    public List<GenericInteractionEntity> comments(@PathVariable GenericInteractionEntity.TargetType targetType, @PathVariable String targetId) {
        return service.comments(targetType, targetId);
    }
}
