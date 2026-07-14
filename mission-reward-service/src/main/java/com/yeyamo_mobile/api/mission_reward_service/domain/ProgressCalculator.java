package com.yeyamo_mobile.api.mission_reward_service.domain;

public final class ProgressCalculator {
    private ProgressCalculator() {}

    public static long next(ObjectiveRule rule, long current, MissionEvent event) {
        if (!matches(rule, event)) return current;
        long candidate = switch (rule.metric()) {
            case COUNT -> current + 1;
            case SUM -> current + number(event, rule.valueField());
            case MAX -> Math.max(current, number(event, rule.valueField()));
        };
        return Math.min(candidate, rule.targetValue());
    }

    public static boolean matches(ObjectiveRule rule, MissionEvent event) {
        if (rule.ruleKey() == null || rule.ruleKey().isBlank()) return true;
        Object actual = event.payload().get(rule.ruleKey());
        return actual != null && String.valueOf(actual).equalsIgnoreCase(rule.ruleValue());
    }

    private static long number(MissionEvent event, String field) {
        Object value = event.payload().get(field);
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception exception) { throw new IllegalArgumentException("Numeric field missing: " + field); }
    }
}
