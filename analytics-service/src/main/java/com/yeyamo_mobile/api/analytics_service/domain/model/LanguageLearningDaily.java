package com.yeyamo_mobile.api.analytics_service.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Daily language learning metrics. Maps to {@code language_learning_daily}. */
@Entity @Table(name = "language_learning_daily")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LanguageLearningDaily {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "aggregation_date", nullable = false) private LocalDate aggregationDate;
    @Column(name = "language_code",    nullable = false, length = 10) private String languageCode;
    @Column(name = "country_code",     length = 2)      private String countryCode;

    @Builder.Default @Column(name = "active_learners")     private Long activeLearners    = 0L;
    @Builder.Default @Column(name = "new_learners")        private Long newLearners       = 0L;
    @Builder.Default @Column(name = "returning_learners")  private Long returningLearners = 0L;
    @Builder.Default @Column(name = "lessons_started")     private Long lessonsStarted    = 0L;
    @Builder.Default @Column(name = "lessons_completed")   private Long lessonsCompleted  = 0L;
    @Column(name = "lesson_completion_rate", precision = 5, scale = 2)
    private BigDecimal lessonCompletionRate;

    @Builder.Default @Column(name = "words_learned")              private Long wordsLearned            = 0L;
    @Builder.Default @Column(name = "daily_words_completed")      private Long dailyWordsCompleted     = 0L;
    @Builder.Default @Column(name = "pronunciation_practices")    private Long pronunciationPractices  = 0L;
    @Builder.Default @Column(name = "quizzes_taken")              private Long quizzesTaken            = 0L;
    @Column(name = "quiz_avg_score", precision = 5, scale = 2)
    private BigDecimal quizAvgScore;

    @Column(name = "avg_session_duration_minutes") private Integer avgSessionDurationMinutes;
    @Builder.Default @Column(name = "total_study_time_minutes") private Long totalStudyTimeMinutes = 0L;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
