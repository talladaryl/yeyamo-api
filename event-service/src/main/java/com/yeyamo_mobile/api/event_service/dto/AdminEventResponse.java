package com.yeyamo_mobile.api.event_service.dto;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.event_service.models.Event;
public record AdminEventResponse(UUID id,UUID placeId,UUID organizerId,UUID partnerId,Long regionId,Long cityId,Long categoryId,String title,String description,Instant startAt,Instant endAt,String status,int capacity,int registeredCount,int availableCapacity,double fillRate,Instant createdAt,Instant updatedAt){
 public static AdminEventResponse from(Event e){int available=Math.max(0,e.getCapacity()-e.getRegisteredCount());double rate=e.getCapacity()==0?0d:(double)e.getRegisteredCount()*100/e.getCapacity();return new AdminEventResponse(e.getId(),e.getPlaceId(),e.getOrganizerId(),e.getPartnerId(),e.getRegionId(),e.getCityId(),e.getCategoryId(),e.getTitle(),e.getDescription(),e.getStartAt(),e.getEndAt(),e.getStatus().name(),e.getCapacity(),e.getRegisteredCount(),available,rate,e.getCreatedAt(),e.getUpdatedAt());}
}
