package com.yeyamo_mobile.api.feed_service.application;

/** The audience source is explicit so a Following response can never be
 * derived by filtering a global feed in the mobile client. */
public enum FeedAudience {
    FOR_YOU,
    FOLLOWING
}
