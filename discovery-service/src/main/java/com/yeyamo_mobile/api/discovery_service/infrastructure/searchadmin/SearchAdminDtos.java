package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.time.Instant;import java.util.*;import jakarta.validation.constraints.*;
public final class SearchAdminDtos {
 private SearchAdminDtos(){}
 public record Overview(String health,int indexes,long documentCount,Instant lastSync,Long latencyMs,double zeroResultRate){}
 public record IndexInfo(String name,String health,long documents,long sizeBytes){}
 public record ReindexResponse(UUID jobId,String status){}
 public record SynonymRequest(@NotNull@Size(min=2,max=20)List<@NotBlank@Size(max=100)String> terms,boolean active){}
 public record SynonymResponse(UUID id,List<String> terms,boolean active,String createdBy,Instant createdAt,Instant updatedAt){}
 public record RankingRequest(@NotNull@Size(max=30)Map<@Pattern(regexp="[A-Za-z][A-Za-z0-9_.-]{1,60}")String,@DecimalMin("0.0")@DecimalMax("100.0")Double> weights){}
 public record RankingResponse(UUID id,int version,Map<String,Double> weights,boolean active,String createdBy,Instant createdAt){}
 public record ZeroResult(String query,String regionCode,long occurrences,Instant firstSeenAt,Instant lastSeenAt){}
}
