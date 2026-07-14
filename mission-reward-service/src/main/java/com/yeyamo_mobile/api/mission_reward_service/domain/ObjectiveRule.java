package com.yeyamo_mobile.api.mission_reward_service.domain;

public record ObjectiveRule(ObjectiveMetric metric, long targetValue, String valueField,
        String ruleKey, String ruleValue) {
    public ObjectiveRule {
        if (metric == null || targetValue <= 0) throw new IllegalArgumentException("Invalid objective rule");
        if (metric != ObjectiveMetric.COUNT && (valueField == null || valueField.isBlank()))
            throw new IllegalArgumentException("valueField is required for SUM and MAX");
    }
}
