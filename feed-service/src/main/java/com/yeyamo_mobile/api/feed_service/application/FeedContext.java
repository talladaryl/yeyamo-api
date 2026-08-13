package com.yeyamo_mobile.api.feed_service.application;

import java.util.*;

public record FeedContext(String countryCode, String cityId, Double latitude, Double longitude,
        Set<String> preferredLanguages, Set<String> followedUsers, Set<String> interests,
        Set<String> viewedCountries, FeedMode mode) {
    public FeedContext {
        if ((latitude == null) != (longitude == null)) throw new IllegalArgumentException("latitude and longitude must be supplied together");
        countryCode = code(countryCode); cityId = blank(cityId);
        preferredLanguages = copy(preferredLanguages, false); followedUsers = copy(followedUsers, false);
        interests = copy(interests, false); viewedCountries = copy(viewedCountries, true);
        mode = mode == null ? FeedMode.COUNTRY : mode;
    }
    public static FeedContext empty() { return new FeedContext(null, null, null, null, Set.of(), Set.of(), Set.of(), Set.of(), FeedMode.COUNTRY); }
    private static Set<String> copy(Set<String> value, boolean upper) { if (value == null) return Set.of(); Set<String> r = new LinkedHashSet<>(); value.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty()).map(v -> upper ? v.toUpperCase(Locale.ROOT) : v).forEach(r::add); return Set.copyOf(r); }
    private static String code(String value) { String trimmed = blank(value); return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT); }
    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
