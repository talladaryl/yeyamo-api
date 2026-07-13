package com.yeyamo_mobile.api.interaction_service.application;
import java.util.UUID;
public record InteractionSummary(UUID postId,long likes,long comments,long shares,boolean likedByViewer,boolean favoriteByViewer){
 public record Counts(long likes,long comments,long shares){}
}
