package com.yeyamo_mobile.api.recommendation_service.application.scoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recommendation.scoring")
public class ScoringProperties {
    private double popularityWeight = 1;
    private double proximityWeight = 1;
    private double preferencesWeight = 1;
    private double historyWeight = 1;

    public double weight(String component) {
        return switch (component) {
            case "popularity" -> popularityWeight;
            case "proximity" -> proximityWeight;
            case "preferences" -> preferencesWeight;
            case "history" -> historyWeight;
            default -> 1;
        };
    }

    public double getPopularityWeight() { return popularityWeight; }
    public void setPopularityWeight(double value) { popularityWeight = nonNegative(value); }
    public double getProximityWeight() { return proximityWeight; }
    public void setProximityWeight(double value) { proximityWeight = nonNegative(value); }
    public double getPreferencesWeight() { return preferencesWeight; }
    public void setPreferencesWeight(double value) { preferencesWeight = nonNegative(value); }
    public double getHistoryWeight() { return historyWeight; }
    public void setHistoryWeight(double value) { historyWeight = nonNegative(value); }
    private double nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Scoring weights must be finite and non-negative");
        return value;
    }
}
