package com.yeyamo_mobile.api.feed_service.application.port;

import java.util.List;

/** Resolves the authenticated viewer's followed content-author subjects. */
public interface FollowingAuthorResolver {
    List<String> followedAuthorIds(String viewerAuthUserId, String bearerToken, String correlationId);
}
