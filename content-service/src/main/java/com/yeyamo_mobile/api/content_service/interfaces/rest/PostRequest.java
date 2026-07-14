package com.yeyamo_mobile.api.content_service.interfaces.rest;
import java.util.*;import com.yeyamo_mobile.api.content_service.domain.model.PostVisibility;import jakarta.validation.constraints.*;
public record PostRequest(@Size(max=5000)String caption,PostVisibility visibility,UUID catalogAssetId,@Size(max=10)List<UUID> mediaIds,@Size(max=20)Set<@Size(max=51)String> hashtags){}
