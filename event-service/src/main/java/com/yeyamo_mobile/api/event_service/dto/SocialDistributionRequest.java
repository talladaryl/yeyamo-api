package com.yeyamo_mobile.api.event_service.dto;

/**
 * Desired social distribution. Omitting this object remains equivalent to
 * requesting neither target.
 */
public record SocialDistributionRequest(boolean publishToFeed, boolean publishToStory) {
}
