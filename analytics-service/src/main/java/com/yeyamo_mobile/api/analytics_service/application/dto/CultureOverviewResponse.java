package com.yeyamo_mobile.api.analytics_service.application.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Top-level culture analytics overview. No PII — aggregate counts only. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CultureOverviewResponse {

    private PeriodMetrics today;
    private PeriodMetrics last7Days;
    private PeriodMetrics last30Days;
    private List<LanguageStat>  topLanguages;
    private List<ArtworkStat>   trendingArtworks;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PeriodMetrics {
        private Long       totalViews;
        private Long       lessonsCompleted;
        private Long       wordsLearned;
        private Long       contributions;
        private Long       artworksSold;
        private BigDecimal revenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LanguageStat {
        private String  languageCode;
        private String  languageName;
        private Long    activeLearners;
        private Long    lessonsCompleted;
        private BigDecimal completionRate;
        private Integer rank;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ArtworkStat {
        private String  artworkId;
        private String  artisanId;
        private Long    views;
        private Long    likes;
        private Long    purchases;
        private Integer rank;
    }
}
