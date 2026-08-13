package com.yeyamo_mobile.api.culture_service.application;

import static com.yeyamo_mobile.api.culture_service.application.CultureDtos.*;
import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;

import com.yeyamo.foundation.domain.CountryReference;
import com.yeyamo.foundation.domain.LanguageReference;
import com.yeyamo_mobile.api.culture_service.domain.CultureContent;
import com.yeyamo_mobile.api.culture_service.domain.CultureTranslation;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.Contents;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.Translations;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.shared.country.CountryConfigClient;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryFeature;

@Service
@Transactional
public class CultureContentService {
    private final Contents contents;
    private final Translations translations;
    private final CultureEventPublisher events;
    private final CountryConfigClient countries;

    public CultureContentService(Contents contents, Translations translations, CultureEventPublisher events) {
        this(contents, translations, events, null);
    }

    @Autowired
    public CultureContentService(Contents contents, Translations translations, CultureEventPublisher events, CountryConfigClient countries) {
        this.contents = contents;
        this.translations = translations;
        this.events = events;
        this.countries = countries;
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> publicList(ContentType type, String country, String area, String city, String language,
            String community, Boolean verified, String search, Pageable pageable) {
        Specification<CultureContent> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), ContentStatus.PUBLISHED));
            predicates.add(builder.equal(root.get("visibility"), Visibility.PUBLIC));
            predicates.add(root.get("sensitivityLevel").in(SensitivityLevel.PUBLIC, SensitivityLevel.CONTEXT_REQUIRED));
            if (type != null) predicates.add(builder.equal(root.get("type"), type));
            if (text(country) != null) predicates.add(builder.equal(root.get("countryCode"), country.toUpperCase()));
            if (text(area) != null) predicates.add(builder.equal(root.get("adminLevel1Id"), area));
            if (text(city) != null) predicates.add(builder.equal(root.get("cityId"), city));
            if (text(language) != null) predicates.add(builder.equal(root.get("primaryLanguageCode"), new LanguageReference(language).languageCode()));
            if (text(community) != null) predicates.add(builder.equal(root.get("communityName"), community));
            if (Boolean.TRUE.equals(verified)) predicates.add(builder.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED));
            if (text(search) != null) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("slug")), like),
                        builder.like(builder.lower(root.get("communityName")), like)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return contents.findAll(specification, pageable).map(ContentResponse::from);
    }

    @Transactional(readOnly = true)
    public ContentResponse publicGet(UUID id) {
        return ContentResponse.from(contents.findByIdAndStatusAndVisibility(id, ContentStatus.PUBLISHED, Visibility.PUBLIC)
                .filter(content -> content.getSensitivityLevel() != SensitivityLevel.SACRED
                        && content.getSensitivityLevel() != SensitivityLevel.COMMUNITY_RESTRICTED)
                .orElseThrow(this::notFound));
    }

    @Transactional(readOnly = true)
    public List<TranslationResponse> publicTranslations(UUID id) {
        publicGet(id);
        return translations.findByContentIdOrderByLanguageCode(id).stream()
                .filter(translation -> translation.getStatus() == TranslationStatus.VERIFIED)
                .map(TranslationResponse::from).toList();
    }

    public ContentResponse create(ContentRequest request, String actor, boolean admin) {
        validate(request);
        validateCountryFeature(request.countryCode());
        if (contents.existsBySlug(request.slug())) {
            throw new CultureException("CULTURE_SLUG_EXISTS", "Ce slug existe déjà", HttpStatus.CONFLICT);
        }
        CultureContent content = CultureContent.create(request.type(), request.slug().trim(),
                new LanguageReference(request.primaryLanguageCode()).languageCode(),
                new CountryReference(request.countryCode()).countryCode(), actor,
                admin ? ContributorType.ADMIN : request.contributorType(), request.sourceType(), request.sensitivityLevel(),
                request.visibility());
        content.updateLocation(request.adminLevel1Id(), request.adminLevel2Id(), request.cityId(), request.localityId(),
                request.communityName());
        contents.save(content);
        TranslationRequest translation = request.translation();
        translations.save(CultureTranslation.create(content.getId(), new LanguageReference(translation.languageCode()).languageCode(),
                translation.title(), translation.summary(), translation.body(), actor));
        events.publish("CultureContentCreated", content.getId(),
                Map.of("contentId", content.getId(), "type", content.getType(), "countryCode", content.getCountryCode(),
                        "contributorId", actor));
        events.publish("CultureTranslationAdded", content.getId(),
                Map.of("contentId", content.getId(), "languageCode", translation.languageCode(), "translatorId", actor));
        return ContentResponse.from(content);
    }

    public ContentResponse update(UUID id, ContentRequest request, String actor, boolean admin) {
        CultureContent content = require(id);
        if (!admin && !content.getCreatedBy().equals(actor)) {
            throw new CultureException("FORBIDDEN", "Contribution inaccessible", HttpStatus.FORBIDDEN);
        }
        content.updateLocation(request.adminLevel1Id(), request.adminLevel2Id(), request.cityId(), request.localityId(),
                request.communityName());
        events.publish("CultureContentUpdated", id, Map.of("contentId", id, "contributorId", content.getCreatedBy()));
        return ContentResponse.from(content);
    }

    public ContentResponse submit(UUID id, String actor) {
        CultureContent content = require(id);
        try {
            content.submit(actor);
        } catch (SecurityException exception) {
            throw new CultureException("FORBIDDEN", exception.getMessage(), HttpStatus.FORBIDDEN);
        }
        events.publish("CultureContributionSubmitted", id,
                Map.of("contentId", id, "contributorId", actor, "userId", actor));
        return ContentResponse.from(content);
    }

    public ContentResponse status(UUID id, StatusRequest request) {
        CultureContent content = require(id);
        if (request.verified()) content.markVerified();
        content.changeStatus(request.status());
        String eventType = switch (request.status()) {
            case PUBLISHED -> "CultureContentPublished";
            case ARCHIVED -> "CultureContentArchived";
            default -> "CultureContentUpdated";
        };
        events.publish(eventType, id, Map.of("contentId", id, "status", request.status(),
                "contributorId", content.getCreatedBy(), "countryCode", content.getCountryCode()));
        return ContentResponse.from(content);
    }

    public ContentResponse review(UUID id, ReviewRequest request) {
        return review(id, request, "system");
    }

    public ContentResponse review(UUID id, ReviewRequest request, String reviewer) {
        CultureContent content = require(id);
        if (content.getStatus() == ContentStatus.SUBMITTED) content.changeStatus(ContentStatus.UNDER_REVIEW);
        if (request.verified()) content.markVerified();
        if (!Set.of(ContentStatus.APPROVED, ContentStatus.CORRECTIONS_REQUIRED, ContentStatus.REJECTED)
                .contains(request.decision())) {
            throw new CultureException("INVALID_REVIEW_DECISION", "Décision invalide", HttpStatus.BAD_REQUEST);
        }
        content.changeStatus(request.decision());
        Map<String, Object> payload = Map.of("contentId", id, "contributorId", content.getCreatedBy(),
                "reviewerId", reviewer, "reason", request.reason());
        if (request.decision() == ContentStatus.APPROVED) events.publish("CultureContributionApproved", id, payload);
        if (request.decision() == ContentStatus.REJECTED) events.publish("CultureContributionRejected", id, payload);
        return ContentResponse.from(content);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> mine(String actor, Pageable pageable) {
        return contents.findByCreatedBy(actor, pageable).map(ContentResponse::from);
    }

    @Transactional(readOnly = true)
    public ContentResponse owned(UUID id, String actor) {
        CultureContent content = require(id);
        if (!content.getCreatedBy().equals(actor)) {
            throw new CultureException("FORBIDDEN", "Contribution inaccessible", HttpStatus.FORBIDDEN);
        }
        return ContentResponse.from(content);
    }

    public void deleteDraft(UUID id, String actor) {
        CultureContent content = require(id);
        if (!content.getCreatedBy().equals(actor)) {
            throw new CultureException("FORBIDDEN", "Contribution inaccessible", HttpStatus.FORBIDDEN);
        }
        if (content.getStatus() != ContentStatus.DRAFT) {
            throw new CultureException("INVALID_STATE", "Seul un brouillon peut être supprimé", HttpStatus.CONFLICT);
        }
        contents.delete(content);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> adminList(Pageable pageable) {
        return contents.findAll(pageable).map(ContentResponse::from);
    }

    @Transactional(readOnly = true)
    public ContentResponse adminGet(UUID id) {
        return ContentResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> adminContributions(Pageable pageable) {
        return contents.findByContributorTypeNot(ContributorType.ADMIN, pageable).map(ContentResponse::from);
    }

    public TranslationResponse verifyTranslation(UUID id) {
        return verifyTranslation(id, "system");
    }

    public TranslationResponse verifyTranslation(UUID id, String reviewer) {
        CultureTranslation translation = translations.findById(id)
                .orElseThrow(() -> new CultureException("TRANSLATION_NOT_FOUND", "Traduction introuvable", HttpStatus.NOT_FOUND));
        translation.verify();
        events.publish("CultureTranslationVerified", translation.getContentId(),
                Map.of("contentId", translation.getContentId(), "translationId", id,
                        "languageCode", translation.getLanguageCode(), "translatorId", translation.getTranslatorId(),
                        "verifierId", reviewer));
        return TranslationResponse.from(translation);
    }

    private CultureContent require(UUID id) {
        return contents.findById(id).orElseThrow(this::notFound);
    }

    private CultureException notFound() {
        return new CultureException("CULTURE_CONTENT_NOT_FOUND", "Contenu culturel introuvable", HttpStatus.NOT_FOUND);
    }

    private void validate(ContentRequest request) {
        new CountryReference(request.countryCode());
        new LanguageReference(request.primaryLanguageCode());
        if (request.sensitivityLevel() == SensitivityLevel.SACRED && request.visibility() == Visibility.PUBLIC) {
            throw new CultureException("SENSITIVE_VISIBILITY_INVALID",
                    "Un contenu sacré ne peut pas être public avant validation", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCountryFeature(String countryCode) {
        if (countries == null) return;
        try { countries.validateFeature(countryCode, CountryFeature.CULTURE_MODULE); }
        catch (CountryConfigClient.CountryConfigException exception) {
            throw new CultureException("COUNTRY_CONFIGURATION_REJECTED", exception.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
