package com.yeyamo_mobile.api.interaction_service.application.port;
import java.util.*;import com.yeyamo_mobile.api.interaction_service.application.InteractionSummary;
public interface InteractionCachePort{Optional<InteractionSummary.Counts> getCounts(UUID postId);void putCounts(UUID postId,InteractionSummary.Counts counts);void evict(UUID postId);}
