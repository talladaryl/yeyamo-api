package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.recommendation_service.application.RecommendationQueryService;
import com.yeyamo_mobile.api.recommendation_service.application.port.RecommendationProjectionPort;
import com.yeyamo_mobile.api.recommendation_service.domain.AdventureRankedCandidate;
import com.yeyamo_mobile.api.recommendation_service.domain.Candidate;
import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;
import com.yeyamo_mobile.shared.country.CountryConfigClient;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryConfig;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryConfigException;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanDayEntity;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanEntity;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanRepository;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanItemEntity;

/**
 * Source of truth for Explorer Adventure Plans.  It never accepts targets or
 * display snapshots from a client: a persisted plan is reconstructed from the
 * current Recommendation candidate projections.
 */
@Service
public class AdventurePlanService {
    private static final Logger log = LoggerFactory.getLogger(AdventurePlanService.class);
    static final int MAX_DAYS = 31;
    static final int MAX_CANDIDATES = 200;
    static final int MAX_ITEMS_PER_DAY = 3;

    private final AdventurePlanRepository plans;
    private final RecommendationProjectionPort projections;
    private final RecommendationQueryService ranking;
    private final CountryConfigClient countries;

    public AdventurePlanService(AdventurePlanRepository plans, RecommendationProjectionPort projections,
            RecommendationQueryService ranking, CountryConfigClient countries) {
        this.plans = plans;
        this.projections = projections;
        this.ranking = ranking;
        this.countries = countries;
    }

    @Transactional(readOnly = true)
    public AdventurePlanPreviewResponse preview(String userId, AdventurePlanRequest request) {
        return generate(userId, normalize(request), Set.of()).response();
    }

    /** Strategy A: recompute server-side rather than accepting a client preview. */
    @Transactional
    public AdventurePlanDetailResponse create(String userId, AdventurePlanRequest request) {
        Criteria criteria = normalize(request);
        GeneratedPreview generated = generate(userId, criteria, Set.of());
        AdventurePlanEntity plan = new AdventurePlanEntity();
        plan.setId(UUID.randomUUID());
        plan.setUserId(userId);
        applyCriteria(plan, criteria);
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(plan.getCreatedAt());

        for (AdventureDayResponse dayResponse : generated.response().days()) {
            AdventurePlanDayEntity day = new AdventurePlanDayEntity();
            day.setId(UUID.randomUUID());
            day.setDate(dayResponse.date());
            day.setPosition(dayResponse.position());
            plan.addDay(day);
            for (int position = 0; position < dayResponse.items().size(); position++) {
                AdventureRecommendationResponse recommendation = dayResponse.items().get(position);
                Candidate candidate = generated.candidates().get(recommendation.recommendationId());
                if (candidate == null) {
                    throw new IllegalStateException("Generated Adventure recommendation is missing its candidate");
                }
                day.addItem(newItem(recommendation, candidate, position));
            }
        }
        plans.save(plan);
        log.info("Adventure plan saved planId={} userId={} country={} selectedItems={}", plan.getId(), userId,
                plan.getCountryCode(), countItems(plan));
        return detail(plan);
    }

    @Transactional(readOnly = true)
    public Page<AdventurePlanSummaryResponse> list(String userId, Pageable pageable) {
        return plans.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::summary);
    }

    @Transactional(readOnly = true)
    public AdventurePlanDetailResponse get(String userId, UUID planId) {
        return detail(owned(planId, userId));
    }

    @Transactional
    public void delete(String userId, UUID planId) {
        AdventurePlanEntity plan = owned(planId, userId);
        plans.delete(plan);
        log.info("Adventure plan deleted planId={} userId={}", planId, userId);
    }

    @Transactional
    public AdventureSkipResponse skip(String userId, UUID planId, UUID recommendationId) {
        AdventurePlanEntity plan = owned(planId, userId);
        AdventurePlanDayEntity sourceDay = null;
        AdventurePlanItemEntity skipped = null;
        for (AdventurePlanDayEntity day : plan.getDays()) {
            for (AdventurePlanItemEntity item : day.getItems()) {
                if (recommendationId.equals(item.getRecommendationId()) && item.getSkippedAt() == null) {
                    sourceDay = day;
                    skipped = item;
                    break;
                }
            }
            if (skipped != null) break;
        }
        if (skipped == null || sourceDay == null) {
            throw notFound("Adventure recommendation was not found in this plan");
        }

        skipped.setSkippedAt(Instant.now());
        Set<String> excludedSources = new HashSet<>();
        for (AdventurePlanDayEntity day : plan.getDays()) {
            for (AdventurePlanItemEntity item : day.getItems()) {
                excludedSources.add(item.getSourceId());
                excludedSources.add(item.getTargetType().name() + ':' + item.getTargetId());
            }
        }
        GeneratedPreview generated = generate(userId, criteria(plan), excludedSources);
        GeneratedItem replacement = replacementForDay(generated, sourceDay.getDate());
        plan.setUpdatedAt(Instant.now());
        if (replacement == null) {
            plans.save(plan);
            return new AdventureSkipResponse(recommendationId, null, "NO_COMPATIBLE_REPLACEMENT");
        }

        int nextPosition = sourceDay.getItems().stream().mapToInt(AdventurePlanItemEntity::getPosition).max().orElse(-1) + 1;
        sourceDay.addItem(newItem(replacement.response(), replacement.candidate(), nextPosition));
        skipped.setReplacedByRecommendationId(replacement.response().recommendationId());
        plans.save(plan);
        log.info("Adventure plan recommendation skipped planId={} userId={} replaced=true", planId, userId);
        return new AdventureSkipResponse(recommendationId, replacement.response(), null);
    }

    private GeneratedItem replacementForDay(GeneratedPreview generated, LocalDate day) {
        for (GeneratedItem item : generated.itemsByDate().getOrDefault(day, List.of())) return item;
        // Non-event candidates are date-flexible. The first remaining one can safely occupy this day.
        return generated.itemsByDate().values().stream().flatMap(List::stream)
                .filter(item -> item.candidate().kind() != CandidateKind.EVENT).findFirst().orElse(null);
    }

    private GeneratedPreview generate(String userId, Criteria criteria, Set<String> excludedSources) {
        FilterResult filter = filterCandidates(criteria, excludedSources);
        List<AdventureRankedCandidate> ranked = ranking.rankForAdventure(userId, filter.candidates(), criteria.interests());
        Map<LocalDate, List<GeneratedItem>> allocated = allocate(criteria, ranked);
        Map<UUID, Candidate> candidates = new HashMap<>();
        List<AdventureDayResponse> days = new ArrayList<>();
        for (int position = 0; position < criteria.dayCount(); position++) {
            LocalDate date = criteria.startDate().plusDays(position);
            List<GeneratedItem> items = allocated.getOrDefault(date, List.of());
            items.forEach(item -> candidates.put(item.response().recommendationId(), item.candidate()));
            days.add(new AdventureDayResponse(date, position, items.stream().map(GeneratedItem::response).toList()));
        }
        List<String> warnings = new ArrayList<>();
        if (filter.unknownPriceExcluded()) warnings.add("UNKNOWN_PRICE_EXCLUDED_BY_STRICT_BUDGET");
        if (criteria.hasTimeWindow() && allocated.values().stream().flatMap(List::stream)
                .anyMatch(item -> item.response().availabilityStatus() == AdventureAvailabilityStatus.UNKNOWN)) {
            warnings.add("OPENING_HOURS_UNKNOWN");
        }
        int selected = candidates.size();
        log.info("Adventure preview userId={} country={} candidates={} filtered={} selected={}", userId,
                criteria.countryCode(), MAX_CANDIDATES, filter.candidates().size(), selected);
        AdventurePlanPreviewResponse response = new AdventurePlanPreviewResponse(criteria.response(), days, List.copyOf(warnings),
                List.of(), selected == 0 ? "NO_COMPATIBLE_CANDIDATES" : null);
        return new GeneratedPreview(response, Map.copyOf(candidates), immutableAllocation(allocated));
    }

    private Map<LocalDate, List<GeneratedItem>> immutableAllocation(Map<LocalDate, List<GeneratedItem>> allocation) {
        Map<LocalDate, List<GeneratedItem>> result = new LinkedHashMap<>();
        allocation.forEach((date, items) -> result.put(date, List.copyOf(items)));
        return Map.copyOf(result);
    }

    private FilterResult filterCandidates(Criteria criteria, Set<String> excludedSources) {
        boolean strictBudget = criteria.minimumAmount() != null || criteria.maximumAmount() != null;
        boolean unknownPriceExcluded = false;
        List<Candidate> result = new ArrayList<>();
        for (Candidate candidate : projections.activeCandidates(MAX_CANDIDATES)) {
            if (!candidate.active() || !criteria.countryCode().equals(candidate.countryCode())
                    || excludedSources.contains(candidate.sourceId()) || targetType(candidate).isEmpty()) continue;
            if (excludedSources.contains(targetKey(candidate))) continue;
            if (candidate.kind() == CandidateKind.EVENT && !matchesEventWindow(candidate, criteria)) continue;
            if (strictBudget && !matchesBudget(candidate, criteria)) {
                if (candidate.price() == null || candidate.currencyCode() == null) unknownPriceExcluded = true;
                continue;
            }
            result.add(candidate);
        }
        return new FilterResult(List.copyOf(result), unknownPriceExcluded);
    }

    private boolean matchesBudget(Candidate candidate, Criteria criteria) {
        if (candidate.price() == null || candidate.currencyCode() == null || criteria.currencyCode() == null
                || !criteria.currencyCode().equals(candidate.currencyCode())) return false;
        if (criteria.minimumAmount() != null && candidate.price().compareTo(criteria.minimumAmount()) < 0) return false;
        return criteria.maximumAmount() == null || candidate.price().compareTo(criteria.maximumAmount()) <= 0;
    }

    private boolean matchesEventWindow(Candidate candidate, Criteria criteria) {
        if (candidate.startsAt() == null) return false;
        LocalDate eventDate = candidate.startsAt().atZone(criteria.zone()).toLocalDate();
        if (eventDate.isBefore(criteria.startDate()) || eventDate.isAfter(criteria.endDate())) return false;
        if (!criteria.hasTimeWindow()) return true;
        var eventStart = candidate.startsAt().atZone(criteria.zone());
        var eventEnd = candidate.endsAt() == null ? eventStart : candidate.endsAt().atZone(criteria.zone());
        // V1 does not support a requested window crossing midnight, therefore neither can a fitting event.
        if (!eventEnd.toLocalDate().equals(eventStart.toLocalDate())) return false;
        LocalTime starts = eventStart.toLocalTime();
        LocalTime ends = eventEnd.toLocalTime();
        return !starts.isBefore(criteria.startTime()) && !ends.isAfter(criteria.endTime());
    }

    private Map<LocalDate, List<GeneratedItem>> allocate(Criteria criteria, List<AdventureRankedCandidate> ranked) {
        Map<LocalDate, List<GeneratedItem>> dates = new LinkedHashMap<>();
        for (int i = 0; i < criteria.dayCount(); i++) dates.put(criteria.startDate().plusDays(i), new ArrayList<>());
        Set<String> selectedTargets = new HashSet<>();
        for (AdventureRankedCandidate candidate : ranked) {
            if (candidate.candidate().kind() != CandidateKind.EVENT) continue;
            LocalDate date = candidate.candidate().startsAt().atZone(criteria.zone()).toLocalDate();
            List<GeneratedItem> items = dates.get(date);
            if (items != null && items.size() < MAX_ITEMS_PER_DAY && selectedTargets.add(targetKey(candidate.candidate()))) {
                items.add(toGeneratedItem(candidate));
            }
        }
        int nextDay = 0;
        for (AdventureRankedCandidate candidate : ranked) {
            if (candidate.candidate().kind() == CandidateKind.EVENT || !selectedTargets.add(targetKey(candidate.candidate()))) continue;
            int selectedDay = nextFreeDay(dates, nextDay);
            if (selectedDay < 0) break;
            LocalDate date = criteria.startDate().plusDays(selectedDay);
            dates.get(date).add(toGeneratedItem(candidate));
            nextDay = (selectedDay + 1) % criteria.dayCount();
        }
        return dates;
    }

    private int nextFreeDay(Map<LocalDate, List<GeneratedItem>> dates, int start) {
        List<LocalDate> values = new ArrayList<>(dates.keySet());
        for (int offset = 0; offset < values.size(); offset++) {
            int index = (start + offset) % values.size();
            if (dates.get(values.get(index)).size() < MAX_ITEMS_PER_DAY) return index;
        }
        return -1;
    }

    private GeneratedItem toGeneratedItem(AdventureRankedCandidate ranked) {
        Candidate candidate = ranked.candidate();
        AdventureTargetType targetType = targetType(candidate).orElseThrow();
        List<String> reasons = new ArrayList<>();
        reasons.add("COUNTRY_MATCH");
        if (ranked.score().components().containsKey("adventure_interest")) reasons.add("INTEREST_MATCH");
        if (candidate.kind() == CandidateKind.EVENT) reasons.add("EVENT_TIME_WINDOW_MATCH");
        AdventureRecommendationResponse response = new AdventureRecommendationResponse(UUID.randomUUID(), targetType,
                candidate.targetId(), candidate.kind() == CandidateKind.EVENT ? candidate.startsAt() : null, List.copyOf(reasons),
                candidate.title(), candidate.imageMediaId(), candidate.locationLabel(), candidate.startsAt(), candidate.endsAt(),
                candidate.price(), candidate.currencyCode(), AdventureAvailabilityStatus.UNKNOWN);
        return new GeneratedItem(response, candidate);
    }

    private Optional<AdventureTargetType> targetType(Candidate candidate) {
        return switch (candidate.kind()) {
            case PLACE, DESTINATION -> Optional.of(AdventureTargetType.PLACE);
            case EVENT -> Optional.of(AdventureTargetType.EVENT);
            case EXPERIENCE -> Optional.of(AdventureTargetType.ACTIVITY);
            case CULTURE, LANGUAGE, TRADITION -> Optional.of(AdventureTargetType.CULTURE_CONTENT);
            default -> Optional.empty();
        };
    }

    private String targetKey(Candidate candidate) {
        return targetType(candidate).orElseThrow().name() + ':' + candidate.targetId();
    }

    private Criteria normalize(AdventurePlanRequest request) {
        if (request == null) throw invalid("INVALID_REQUEST", "Adventure plan criteria are required");
        if (request.endDate().isBefore(request.startDate())) throw invalid("INVALID_DATE_RANGE", "endDate must be on or after startDate");
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (days > MAX_DAYS) throw invalid("DATE_RANGE_TOO_LARGE", "An Adventure Plan is limited to " + MAX_DAYS + " days");
        if ((request.startTime() == null) != (request.endTime() == null)) {
            throw invalid("INVALID_TIME_RANGE", "startTime and endTime must be provided together");
        }
        if (request.startTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw invalid("INVALID_TIME_RANGE", "endTime must be after startTime");
        }
        String country = request.countryCode().trim().toUpperCase(Locale.ROOT);
        CountryConfig configuration;
        try {
            configuration = countries.getCountry(country);
            if (!"LIVE".equals(configuration.launchStatus()) && !"BETA".equals(configuration.launchStatus())) {
                throw invalid("COUNTRY_NOT_AVAILABLE", "Country " + country + " is not operational");
            }
        } catch (CountryConfigException exception) {
            throw invalid("COUNTRY_NOT_AVAILABLE", exception.getMessage());
        }
        ZoneId zone = zone(configuration.defaultTimezone());
        AdventureBudgetRequest budget = request.budget();
        BigDecimal min = budget == null ? null : budget.minimumAmount();
        BigDecimal max = budget == null ? null : budget.maximumAmount();
        if (min != null && min.signum() < 0 || max != null && max.signum() < 0) {
            throw invalid("INVALID_BUDGET", "Budget amounts cannot be negative");
        }
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw invalid("INVALID_BUDGET", "maximumAmount must be greater than or equal to minimumAmount");
        }
        String currency = budget == null || budget.currencyCode() == null ? null : budget.currencyCode().trim().toUpperCase(Locale.ROOT);
        if (min != null || max != null) {
            if (currency == null || currency.isBlank()) throw invalid("INVALID_BUDGET", "currencyCode is required with a budget amount");
        }
        if (currency != null && !currency.isBlank()) {
            try { countries.validateCurrency(country, currency); }
            catch (CountryConfigException exception) { throw invalid("INVALID_CURRENCY", exception.getMessage()); }
        }
        Set<String> interests = new LinkedHashSet<>();
        if (request.interestCodes() != null) request.interestCodes().stream().filter(Objects::nonNull).map(String::trim)
                .filter(value -> !value.isEmpty()).map(value -> value.toLowerCase(Locale.ROOT)).forEach(interests::add);
        return new Criteria(country, request.startDate(), request.endDate(), request.startTime(), request.endTime(), request.partyType(),
                Set.copyOf(interests), budget == null ? null : budget.tier(), min, max, blankToNull(currency), zone);
    }

    private ZoneId zone(String countryTimezone) {
        try { return countryTimezone == null || countryTimezone.isBlank() ? ZoneId.of("UTC") : ZoneId.of(countryTimezone); }
        catch (RuntimeException exception) { return ZoneId.of("UTC"); }
    }

    private AdventurePlanEntity owned(UUID id, String userId) {
        return plans.findDetailByIdAndUserId(id, userId).orElseThrow(() -> notFound("Adventure plan was not found"));
    }

    private AdventurePlanException invalid(String code, String message) { return new AdventurePlanException(code, message, HttpStatus.BAD_REQUEST); }
    private AdventurePlanException notFound(String message) { return new AdventurePlanException("ADVENTURE_PLAN_NOT_FOUND", message, HttpStatus.NOT_FOUND); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private void applyCriteria(AdventurePlanEntity plan, Criteria criteria) {
        plan.setCountryCode(criteria.countryCode()); plan.setCountryTimezone(criteria.zone().getId());
        plan.setStartDate(criteria.startDate()); plan.setEndDate(criteria.endDate());
        plan.setStartTime(criteria.startTime()); plan.setEndTime(criteria.endTime()); plan.setPartyType(criteria.partyType());
        plan.setInterestCodes(criteria.interests()); plan.setBudgetTier(criteria.budgetTier()); plan.setBudgetMinimum(criteria.minimumAmount());
        plan.setBudgetMaximum(criteria.maximumAmount()); plan.setCurrencyCode(criteria.currencyCode());
    }

    private Criteria criteria(AdventurePlanEntity plan) {
        return new Criteria(plan.getCountryCode(), plan.getStartDate(), plan.getEndDate(), plan.getStartTime(), plan.getEndTime(),
                plan.getPartyType(), Set.copyOf(plan.getInterestCodes()), plan.getBudgetTier(), plan.getBudgetMinimum(),
                plan.getBudgetMaximum(), plan.getCurrencyCode(), zone(plan.getCountryTimezone()));
    }

    private AdventurePlanItemEntity newItem(AdventureRecommendationResponse response, Candidate candidate, int position) {
        AdventurePlanItemEntity item = new AdventurePlanItemEntity();
        item.setId(UUID.randomUUID()); item.setRecommendationId(response.recommendationId()); item.setSourceId(candidate.sourceId());
        item.setTargetType(response.targetType()); item.setTargetId(response.targetId()); item.setScheduledAt(response.scheduledAt());
        item.setPosition(position); item.setSnapshotTitle(response.title()); item.setImageMediaId(response.imageMediaId());
        item.setLocationLabel(response.locationLabel()); item.setStartsAt(response.startsAt()); item.setEndsAt(response.endsAt());
        item.setPrice(response.price()); item.setCurrencyCode(response.currencyCode()); item.setAvailabilityStatus(response.availabilityStatus());
        item.setReasonCodes(String.join(",", response.reasonCodes()));
        return item;
    }

    private AdventurePlanDetailResponse detail(AdventurePlanEntity plan) {
        List<AdventureDayResponse> days = plan.getDays().stream().sorted(Comparator.comparingInt(AdventurePlanDayEntity::getPosition))
                .map(day -> new AdventureDayResponse(day.getDate(), day.getPosition(), day.getItems().stream()
                        .filter(item -> item.getSkippedAt() == null)
                        .sorted(Comparator.comparingInt(AdventurePlanItemEntity::getPosition)).map(this::response).toList())).toList();
        return new AdventurePlanDetailResponse(plan.getId(), criteriaResponse(plan), days, plan.getCreatedAt(), plan.getUpdatedAt());
    }

    private AdventurePlanSummaryResponse summary(AdventurePlanEntity plan) {
        return new AdventurePlanSummaryResponse(plan.getId(), plan.getCountryCode(), plan.getStartDate(), plan.getEndDate(),
                plan.getPartyType(), plan.getBudgetTier(), plan.getBudgetMinimum(), plan.getBudgetMaximum(), plan.getCurrencyCode(),
                countItems(plan), plan.getCreatedAt());
    }

    private int countItems(AdventurePlanEntity plan) {
        return plan.getDays().stream().mapToInt(day -> (int) day.getItems().stream()
                .filter(item -> item.getSkippedAt() == null).count()).sum();
    }

    private AdventureRecommendationResponse response(AdventurePlanItemEntity item) {
        List<String> reasons = item.getReasonCodes() == null || item.getReasonCodes().isBlank() ? List.of()
                : List.of(item.getReasonCodes().split(","));
        return new AdventureRecommendationResponse(item.getRecommendationId(), item.getTargetType(), item.getTargetId(), item.getScheduledAt(),
                reasons, item.getSnapshotTitle(), item.getImageMediaId(), item.getLocationLabel(), item.getStartsAt(), item.getEndsAt(),
                item.getPrice(), item.getCurrencyCode(), item.getAvailabilityStatus());
    }

    private AdventureCriteriaResponse criteriaResponse(AdventurePlanEntity plan) {
        return new AdventureCriteriaResponse(plan.getCountryCode(), plan.getStartDate(), plan.getEndDate(), plan.getStartTime(),
                plan.getEndTime(), plan.getPartyType(), plan.getInterestCodes().stream().sorted().toList(), plan.getBudgetTier(),
                plan.getBudgetMinimum(), plan.getBudgetMaximum(), plan.getCurrencyCode());
    }

    private record FilterResult(List<Candidate> candidates, boolean unknownPriceExcluded) {}
    private record GeneratedItem(AdventureRecommendationResponse response, Candidate candidate) {}
    private record GeneratedPreview(AdventurePlanPreviewResponse response, Map<UUID, Candidate> candidates,
            Map<LocalDate, List<GeneratedItem>> itemsByDate) {}
    private record Criteria(String countryCode, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime,
            AdventurePartyType partyType, Set<String> interests, AdventureBudgetTier budgetTier, BigDecimal minimumAmount,
            BigDecimal maximumAmount, String currencyCode, ZoneId zone) {
        int dayCount() { return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1); }
        boolean hasTimeWindow() { return startTime != null; }
        AdventureCriteriaResponse response() { return new AdventureCriteriaResponse(countryCode, startDate, endDate, startTime, endTime,
                partyType, interests.stream().sorted().toList(), budgetTier, minimumAmount, maximumAmount, currencyCode); }
    }
}
