package com.yeyamo_mobile.api.content_service.application;
import java.util.*;import com.yeyamo_mobile.api.content_service.domain.model.PostVisibility;
public record PostCommand(String caption,PostVisibility visibility,UUID catalogAssetId,List<UUID> mediaIds,Set<String> hashtags){}
