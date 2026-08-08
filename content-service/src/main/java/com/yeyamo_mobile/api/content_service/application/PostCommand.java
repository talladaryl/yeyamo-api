package com.yeyamo_mobile.api.content_service.application;
import java.util.*;import com.yeyamo_mobile.api.content_service.domain.model.*;
public record PostCommand(String caption,PostVisibility visibility,UUID catalogAssetId,List<UUID> mediaIds,Set<String> hashtags,PostReferenceType referenceType,String referenceId){
 public PostCommand(String caption,PostVisibility visibility,UUID catalogAssetId,List<UUID>mediaIds,Set<String>hashtags){this(caption,visibility,catalogAssetId,mediaIds,hashtags,catalogAssetId==null?PostReferenceType.NONE:PostReferenceType.PLACE,catalogAssetId==null?null:catalogAssetId.toString());}
}
