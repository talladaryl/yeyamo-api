package com.yeyamo_mobile.api.content_service.application;
import java.util.*;import com.yeyamo_mobile.api.content_service.domain.model.*;
public record PostCommand(String caption,PostVisibility visibility,UUID catalogAssetId,List<UUID> mediaIds,Set<String> hashtags,PostReferenceType referenceType,String referenceId,
 String countryCode,UUID adminLevel1Id,UUID adminLevel2Id,UUID cityId,UUID localityId,Double latitude,Double longitude,String languageCode,CulturalTargetType targetType,UUID targetId){
 public PostCommand(String caption,PostVisibility visibility,UUID catalogAssetId,List<UUID>mediaIds,Set<String>hashtags){this(caption,visibility,catalogAssetId,mediaIds,hashtags,catalogAssetId==null?PostReferenceType.NONE:PostReferenceType.PLACE,catalogAssetId==null?null:catalogAssetId.toString(),null,null,null,null,null,null,null,null,null,null);}
}
