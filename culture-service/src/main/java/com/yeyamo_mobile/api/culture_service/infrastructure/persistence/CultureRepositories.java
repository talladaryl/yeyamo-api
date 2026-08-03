package com.yeyamo_mobile.api.culture_service.infrastructure.persistence;
import com.yeyamo_mobile.api.culture_service.domain.*;import com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;import java.time.LocalDate;import java.util.*;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;
public final class CultureRepositories {private CultureRepositories(){}
 public interface Contents extends JpaRepository<CultureContent,UUID>,JpaSpecificationExecutor<CultureContent>{Optional<CultureContent> findByIdAndStatusAndVisibility(UUID id,ContentStatus status,Visibility visibility);boolean existsBySlug(String slug);Page<CultureContent> findByCreatedBy(String actor,Pageable pageable);Page<CultureContent> findByContributorTypeNot(ContributorType type,Pageable pageable);}
 public interface Translations extends JpaRepository<CultureTranslation,UUID>{List<CultureTranslation> findByContentIdOrderByLanguageCode(UUID id);Optional<CultureTranslation> findFirstByContentIdAndLanguageCodeAndStatus(UUID id,String language,TranslationStatus status);}
 public interface Languages extends JpaRepository<Language,String>{List<Language> findByStatusInOrderByName(List<LanguageStatus> statuses);}
 public interface Lessons extends JpaRepository<LanguageLesson,UUID>{List<LanguageLesson> findByLanguageCodeAndStatusOrderByDisplayOrder(String language,LessonStatus status);}
 public interface Items extends JpaRepository<LanguageLessonItem,UUID>{List<LanguageLessonItem> findByLessonIdOrderByDisplayOrder(UUID lesson);}
 public interface Exercises extends JpaRepository<LanguageExercise,UUID>{List<LanguageExercise> findByLessonIdOrderByDisplayOrder(UUID lesson);}
 public interface Progress extends JpaRepository<LanguageProgress,UUID>{Optional<LanguageProgress> findByUserIdAndLessonId(String user,UUID lesson);List<LanguageProgress> findByUserIdOrderByStartedAtDesc(String user);List<LanguageProgress> findByUserIdAndLanguageCodeOrderByStartedAtDesc(String user,String language);}
 public interface Attempts extends JpaRepository<LanguageAttempt,UUID>{}
 public interface DailyWords extends JpaRepository<DailyWordSelection,Long>{Optional<DailyWordSelection> findBySelectionDateAndCountryCodeAndLanguageCode(LocalDate date,String country,String language);Optional<DailyWordSelection> findFirstBySelectionDateAndCountryCodeAndLanguageCodeIsNull(LocalDate date,String country);}
}
