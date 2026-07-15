package com.yeyamo_mobile.api.gamification_service.domain;import java.time.Instant;import java.util.UUID;public record PassportStamp(UUID id,String userId,String destinationId,Instant stampedAt){}
