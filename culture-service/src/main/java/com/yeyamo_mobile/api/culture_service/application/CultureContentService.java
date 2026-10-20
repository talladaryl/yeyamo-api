package com.yeyamo_mobile.api.culture_service.application;

import static com.yeyamo_mobile.api.culture_service.application.CultureDtos.*;
import static com.yeyamo_mobile.api.culture_service.domain.CultureEnums.*;

import com.yeyamo.foundation.domain.CountryReference;
import com.yeyamo.foundation.domain.LanguageReference;
import com.yeyamo_mobile.api.culture_service.domain.CultureContent;
import com.yeyamo_mobile.api.culture_service.domain.CultureTranslation;
import com.yeyamo_mobile.api.culture_service.domain.Language;
import com.yeyamo_mobile.api.culture_service.domain.ProverbDetailsEntity;
import com.yeyamo_mobile.api.culture_service.domain.RecipeDetailsEntity;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.Contents;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.Languages;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.Translations;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.RecipeRevisions;
import com.yeyamo_mobile.api.culture_service.domain.CultureRecipeRevision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final Languages languages;
    private final RecipeRevisions recipeRevisions;
    private final ObjectMapper objectMapper;
    private final MediaAssociationClient mediaAssociations;

    public CultureContentService(Contents contents, Translations translations, CultureEventPublisher events) {
        this(contents, translations, events, null, null, null, new ObjectMapper().findAndRegisterModules(), null);
    }

    public CultureContentService(Contents contents, Translations translations, CultureEventPublisher events, CountryConfigClient countries) {
        this(contents, translations, events, countries, null, null, new ObjectMapper().findAndRegisterModules(), null);
    }

    public CultureContentService(Contents contents, Translations translations, CultureEventPublisher events, CountryConfigClient countries, Languages languages) {
        this(contents,translations,events,countries,languages,null,new ObjectMapper().findAndRegisterModules(),null);
    }

    public CultureContentService(Contents contents, Translations translations, CultureEventPublisher events, CountryConfigClient countries, Languages languages, RecipeRevisions recipeRevisions, ObjectMapper objectMapper) {
        this(contents, translations, events, countries, languages, recipeRevisions, objectMapper, null);
    }

    @Autowired
    public CultureContentService(Contents contents, Translations translations, CultureEventPublisher events, CountryConfigClient countries, Languages languages, RecipeRevisions recipeRevisions, ObjectMapper objectMapper, MediaAssociationClient mediaAssociations) {
        this.contents = contents;
        this.translations = translations;
        this.events = events;
        this.countries = countries;
        this.languages = languages;
        this.recipeRevisions=recipeRevisions;
        this.objectMapper=objectMapper;
        this.mediaAssociations=mediaAssociations;
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
        applyDetails(content, request, actor, false);
        contents.save(content);
        TranslationRequest translation = request.translation();
        translations.save(CultureTranslation.create(content.getId(), new LanguageReference(translation.languageCode()).languageCode(),
                translation.title(), translation.summary(), translation.body(), actor));
        events.publish("CultureContentCreated", content.getId(), contentEventPayload(content, translation.title(), active(content), actor));
        events.publish("CultureTranslationAdded", content.getId(),
                Map.of("contentId", content.getId(), "languageCode", translation.languageCode(), "translatorId", actor));
        return ContentResponse.from(content);
    }

    public ContentResponse update(UUID id, ContentRequest request, String actor, boolean admin) {
        CultureContent content = require(id);
        if (!admin && !content.getCreatedBy().equals(actor)) {
            throw new CultureException("FORBIDDEN", "Contribution inaccessible", HttpStatus.FORBIDDEN);
        }
        validate(request);
        if(content.getType()==ContentType.RECIPE&&content.getStatus()==ContentStatus.PUBLISHED){
            if(recipeRevisions==null)throw new CultureException("RECIPE_REVISION_UNAVAILABLE","Le stockage des rÃ©visions de recette est indisponible",HttpStatus.SERVICE_UNAVAILABLE);
            if(request.recipeDetails()!=null)YouTubeUrlValidator.normalize(request.recipeDetails().youtubeVideoUrl());
            CultureRecipeRevision revision=recipeRevisions.findFirstByContentIdAndStatusInOrderByCreatedAtDesc(id,Set.of(RecipeRevisionStatus.DRAFT,RecipeRevisionStatus.SUBMITTED,RecipeRevisionStatus.UNDER_REVIEW)).orElse(null);
            String candidate=writeCandidate(request);
            if(revision==null)revision=recipeRevisions.save(CultureRecipeRevision.draft(id,actor,content.getVersion(),candidate));
            else {try{revision.replaceDraft(content.getVersion(),candidate,actor);}catch(SecurityException exception){throw new CultureException("FORBIDDEN",exception.getMessage(),HttpStatus.FORBIDDEN);}catch(IllegalStateException exception){throw new CultureException("REVISION_ALREADY_SUBMITTED",exception.getMessage(),HttpStatus.CONFLICT);}}
            events.publish("CultureRecipeRevisionCreated",id,Map.of("contentId",id,"contributorId",actor,"baseVersion",content.getVersion()));
            return ContentResponse.from(content,revision);
        }
        content.updateLocation(request.adminLevel1Id(), request.adminLevel2Id(), request.cityId(), request.localityId(),
                request.communityName());
        applyDetails(content, request, actor, content.getStatus()==ContentStatus.PUBLISHED);
        events.publish("CultureContentUpdated", id, contentEventPayload(content, title(content), active(content), content.getCreatedBy()));
        return ContentResponse.from(content);
    }

    public ContentResponse submit(UUID id, String actor) {
        CultureContent content = require(id);
        if(content.getType()==ContentType.RECIPE&&content.getStatus()==ContentStatus.PUBLISHED&&recipeRevisions!=null){
            CultureRecipeRevision revision=recipeRevisions.findFirstByContentIdAndStatusInOrderByCreatedAtDesc(id,Set.of(RecipeRevisionStatus.DRAFT)).orElseThrow(()->new CultureException("RECIPE_REVISION_NOT_FOUND","Aucune rÃ©vision de recette Ã  soumettre",HttpStatus.CONFLICT));
            try{revision.submit(actor);}catch(SecurityException exception){throw new CultureException("FORBIDDEN",exception.getMessage(),HttpStatus.FORBIDDEN);}
            events.publish("CultureContributionSubmitted",id,Map.of("contentId",id,"revisionId",revision.getId(),"contributorId",actor,"userId",actor));
            return ContentResponse.from(content,revision);
        }
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
        events.publish(eventType, id, contentEventPayload(content, title(content), active(content), content.getCreatedBy()));
        return ContentResponse.from(content);
    }

    public ContentResponse review(UUID id, ReviewRequest request) {
        return review(id, request, "system");
    }

    public ContentResponse review(UUID id, ReviewRequest request, String reviewer) {
        CultureContent content = require(id);
        if(content.getType()==ContentType.RECIPE&&content.getStatus()==ContentStatus.PUBLISHED&&recipeRevisions!=null){
            if(!Set.of(ContentStatus.APPROVED,ContentStatus.REJECTED).contains(request.decision()))throw new CultureException("INVALID_REVIEW_DECISION","Une rÃ©vision publiÃ©e doit Ãªtre approuvÃ©e ou rejetÃ©e",HttpStatus.BAD_REQUEST);
            CultureRecipeRevision revision=recipeRevisions.findFirstByContentIdAndStatusInOrderByCreatedAtDesc(id,Set.of(RecipeRevisionStatus.SUBMITTED)).orElseThrow(()->new CultureException("RECIPE_REVISION_NOT_FOUND","Aucune rÃ©vision soumise",HttpStatus.CONFLICT));
            revision.beginReview();
            if(request.decision()==ContentStatus.APPROVED){
                if(content.getVersion()!=revision.getBaseVersion())throw new CultureException("RECIPE_REVISION_CONFLICT","La recette publique a changÃ© depuis la crÃ©ation de la rÃ©vision",HttpStatus.CONFLICT);
                ContentRequest candidate=readCandidate(revision.getCandidateJson());validate(candidate);
                content.updateLocation(candidate.adminLevel1Id(),candidate.adminLevel2Id(),candidate.cityId(),candidate.localityId(),candidate.communityName());applyDetails(content,candidate,revision.getAuthorId(),true);
                TranslationRequest requested=candidate.translation();String language=new LanguageReference(requested.languageCode()).languageCode();
                CultureTranslation translation=translations.findByContentIdOrderByLanguageCode(id).stream().filter(value->value.getLanguageCode().equals(language)).findFirst().orElseGet(()->translations.save(CultureTranslation.create(id,language,requested.title(),requested.summary(),requested.body(),revision.getAuthorId())));
                translation.applyApprovedRevision(requested.title(),requested.summary(),requested.body(),revision.getAuthorId());
            }
            revision.decide(request.decision()==ContentStatus.APPROVED,reviewer,request.reason());
            events.publish(request.decision()==ContentStatus.APPROVED?"CultureContributionApproved":"CultureContributionRejected",id,Map.of("contentId",id,"revisionId",revision.getId(),"contributorId",revision.getAuthorId(),"reviewerId",reviewer,"reason",request.reason()));
            return ContentResponse.from(content,revision);
        }
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
        return contents.findByCreatedBy(actor, pageable).map(this::contributorResponse);
    }

    @Transactional(readOnly = true)
    public ContentResponse owned(UUID id, String actor) {
        CultureContent content = require(id);
        if (!content.getCreatedBy().equals(actor)) {
            throw new CultureException("FORBIDDEN", "Contribution inaccessible", HttpStatus.FORBIDDEN);
        }
        return contributorResponse(content);
    }

    public void deleteDraft(UUID id, String actor) {
        CultureContent content = require(id);
        if (!content.getCreatedBy().equals(actor)) {
            throw new CultureException("FORBIDDEN", "Contribution inaccessible", HttpStatus.FORBIDDEN);
        }
        if (content.getStatus() != ContentStatus.DRAFT) {
            throw new CultureException("INVALID_STATE", "Seul un brouillon peut être supprimé", HttpStatus.CONFLICT);
        }
        events.publish("CultureContentDeleted", id, contentEventPayload(content, title(content), false, content.getCreatedBy()));
        contents.delete(content);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> adminList(Pageable pageable) {
        return contents.findAll(pageable).map(ContentResponse::from);
    }

    @Transactional(readOnly = true)
    public ContentResponse adminGet(UUID id) {
        return contributorResponse(require(id));
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

    private ContentResponse contributorResponse(CultureContent content){CultureRecipeRevision revision=recipeRevisions==null?null:recipeRevisions.findFirstByContentIdOrderByCreatedAtDesc(content.getId()).orElse(null);return ContentResponse.from(content,revision);}

    private String writeCandidate(ContentRequest request){try{return objectMapper.writeValueAsString(request);}catch(JsonProcessingException exception){throw new CultureException("RECIPE_REVISION_SERIALIZATION_FAILED","Impossible d'enregistrer la rÃ©vision",HttpStatus.INTERNAL_SERVER_ERROR);}}
    private ContentRequest readCandidate(String json){try{return objectMapper.readValue(json,ContentRequest.class);}catch(JsonProcessingException exception){throw new CultureException("RECIPE_REVISION_INVALID","La rÃ©vision enregistrÃ©e est invalide",HttpStatus.INTERNAL_SERVER_ERROR);}}

    private void validate(ContentRequest request) {
        new CountryReference(request.countryCode());
        new LanguageReference(request.primaryLanguageCode());
        if (request.sensitivityLevel() == SensitivityLevel.SACRED && request.visibility() == Visibility.PUBLIC) {
            throw new CultureException("SENSITIVE_VISIBILITY_INVALID",
                    "Un contenu sacré ne peut pas être public avant validation", HttpStatus.BAD_REQUEST);
        }
        boolean proverb = request.proverbDetails() != null;
        boolean recipe = request.recipeDetails() != null;
        if (proverb && recipe || proverb && request.type() != ContentType.PROVERB || recipe && request.type() != ContentType.RECIPE) {
            throw new CultureException("CULTURE_DETAILS_TYPE_MISMATCH", "Les détails structurés ne correspondent pas au type de contenu", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyDetails(CultureContent content, ContentRequest request, String actor, boolean publication) {
        if (request.proverbDetails() != null) {
            ProverbDetailsRequest details = request.proverbDetails();
            Language language = languages == null ? null : languages.findById(new LanguageReference(details.originLanguageCode()).languageCode())
                    .orElseThrow(() -> new CultureException("ORIGIN_LANGUAGE_NOT_FOUND", "Langue d'origine introuvable", HttpStatus.BAD_REQUEST));
            if (language == null) {
                throw new CultureException("ORIGIN_LANGUAGE_VALIDATION_UNAVAILABLE", "Validation de la langue d'origine indisponible", HttpStatus.SERVICE_UNAVAILABLE);
            }
            content.replaceDetails(ProverbDetailsEntity.create(content, details.literalTranslation(), details.meaning(), language, details.audioUrl()), null);
            return;
        }
        if (request.recipeDetails() != null) {
            RecipeDetailsRequest details = request.recipeDetails();
            RecipeDetailsEntity previous=content.getRecipeDetails();
            Set<UUID> previousIngredientIds=previous==null?Set.of():previous.getIngredients().stream().map(RecipeDetailsEntity.RecipeIngredient::getId).collect(java.util.stream.Collectors.toSet());
            Set<UUID> previousStepIds=previous==null?Set.of():previous.getSteps().stream().map(RecipeDetailsEntity.RecipeStep::getId).collect(java.util.stream.Collectors.toSet());
            List<RecipeDetailsEntity.RecipeIngredient> ingredients=new ArrayList<>();
            for(int index=0;index<details.ingredients().size();index++){
                RecipeIngredientRequest value=details.ingredients().get(index);UUID stable=value.id();
                if(stable!=null&&!previousIngredientIds.contains(stable))throw new CultureException("FOREIGN_RECIPE_INGREDIENT_ID","Identifiant ingrédient étranger à cette recette",HttpStatus.BAD_REQUEST);
                if(stable==null&&previous!=null&&index<previous.getIngredients().size())stable=previous.getIngredients().get(index).getId();
                UUID stableId=stable;RecipeDetailsEntity.RecipeIngredient old=previous==null?null:previous.getIngredients().stream().filter(item->item.getId().equals(stableId)).findFirst().orElse(null);
                ingredients.add(new RecipeDetailsEntity.RecipeIngredient(stable,value.name(),value.quantity(),value.unit(),value.groupName()!=null?value.groupName():old==null?null:old.getGroupName(),value.optional()!=null?value.optional():old!=null&&old.isOptional(),value.substitutionNotes()!=null?value.substitutionNotes():old==null?null:old.getSubstitutionNotes(),value.quantityValue()!=null?value.quantityValue():old==null?null:old.getQuantityValue(),value.quantityMaxValue()!=null?value.quantityMaxValue():old==null?null:old.getQuantityMaxValue(),value.quantityUnitCode()!=null?value.quantityUnitCode():old==null?null:old.getQuantityUnitCode()));
            }
            Map<UUID,RecipeDetailsEntity.RecipeIngredient> ingredientsById=ingredients.stream().collect(java.util.stream.Collectors.toMap(RecipeDetailsEntity.RecipeIngredient::getId,v->v));
            List<RecipeDetailsEntity.RecipeStep> steps=new ArrayList<>();
            for(int index=0;index<details.steps().size();index++){
                RecipeStepRequest value=details.steps().get(index);UUID stable=value.id();
                if(stable!=null&&!previousStepIds.contains(stable))throw new CultureException("FOREIGN_RECIPE_STEP_ID","Identifiant étape étranger à cette recette",HttpStatus.BAD_REQUEST);
                if(stable==null&&previous!=null&&index<previous.getSteps().size())stable=previous.getSteps().get(index).getId();
                UUID stableId=stable;RecipeDetailsEntity.RecipeStep old=previous==null?null:previous.getSteps().stream().filter(item->item.getId().equals(stableId)).findFirst().orElse(null);
                RecipeDetailsEntity.RecipeStep step=new RecipeDetailsEntity.RecipeStep(stable,value.title()!=null?value.title():old==null?null:old.getTitle(),value.instruction(),value.durationMinutes()!=null?value.durationMinutes():old==null?null:old.getDurationMinutes(),value.temperatureValue()!=null?value.temperatureValue():old==null?null:old.getTemperatureValue(),value.temperatureUnit()!=null?value.temperatureUnit():old==null?null:old.getTemperatureUnit(),value.expectedResult()!=null?value.expectedResult():old==null?null:old.getExpectedResult(),value.tip()!=null?value.tip():old==null?null:old.getTip(),value.safetyNote()!=null?value.safetyNote():old==null?null:old.getSafetyNote(),value.mediaId()!=null?value.mediaId():old==null?null:old.getMediaId());
                if(value.ingredientIds()!=null){List<RecipeDetailsEntity.RecipeIngredient> linked=value.ingredientIds().stream().map(ingredientId->{RecipeDetailsEntity.RecipeIngredient found=ingredientsById.get(ingredientId);if(found==null)throw new CultureException("CROSS_RECIPE_INGREDIENT_REFERENCE","Une étape référence un ingrédient absent de la recette",HttpStatus.BAD_REQUEST);return found;}).toList();step.linkIngredients(linked);}
                steps.add(step);
            }
            List<RecipeDetailsEntity.RecipeEquipment> equipment=details.equipment()==null&&previous!=null?previous.getEquipment().stream().map(value->new RecipeDetailsEntity.RecipeEquipment(value.getId(),value.getName(),value.isOptional())).toList():details.equipment()==null?List.of():details.equipment().stream().map(value->new RecipeDetailsEntity.RecipeEquipment(value.id(),value.name(),Boolean.TRUE.equals(value.optional()))).toList();
            List<RecipeDetailsEntity.RecipeMedia> gallery=details.gallery()==null&&previous!=null?previous.getGallery().stream().map(value->new RecipeDetailsEntity.RecipeMedia(value.getId(),value.getMediaId(),value.getRole(),value.getCaption(),value.getAltText())).toList():details.gallery()==null?List.of():details.gallery().stream().map(value->new RecipeDetailsEntity.RecipeMedia(value.id(),value.mediaId(),value.role(),value.caption(),value.altText())).toList();
            List<MediaAssociationClient.Reference> mediaReferences=new ArrayList<>();
            if(details.heroMediaId()!=null)mediaReferences.add(new MediaAssociationClient.Reference(details.heroMediaId(),MediaAssociationClient.Usage.HERO));
            if(details.videoMediaId()!=null)mediaReferences.add(new MediaAssociationClient.Reference(details.videoMediaId(),MediaAssociationClient.Usage.VIDEO));
            details.steps().stream().filter(step->step.mediaId()!=null).forEach(step->mediaReferences.add(new MediaAssociationClient.Reference(step.mediaId(),MediaAssociationClient.Usage.STEP)));
            if(details.gallery()!=null)details.gallery().stream().filter(item->item.mediaId()!=null).forEach(item->mediaReferences.add(new MediaAssociationClient.Reference(item.mediaId(),MediaAssociationClient.Usage.GALLERY)));
            if(!mediaReferences.isEmpty()){
                if(mediaAssociations==null)throw new CultureException("MEDIA_VALIDATION_UNAVAILABLE","Le contrat distant de validation des droits média n'est pas disponible",HttpStatus.SERVICE_UNAVAILABLE);
                mediaAssociations.validate(actor,mediaReferences,publication);
            }
            String youtube=details.youtubeVideoUrl()!=null?YouTubeUrlValidator.normalize(details.youtubeVideoUrl()):previous==null?null:previous.getYoutubeVideoUrl();
            RecipeDetailsEntity recipe=RecipeDetailsEntity.create(content,ingredients,steps,details.prepTimeMinutes(),details.servings());
            recipe.enrich(details.cookTimeMinutes()!=null?details.cookTimeMinutes():previous==null?null:previous.getCookTimeMinutes(),details.restTimeMinutes()!=null?details.restTimeMinutes():previous==null?null:previous.getRestTimeMinutes(),details.difficulty()!=null?details.difficulty():previous==null?null:previous.getDifficulty(),details.yieldText()!=null?details.yieldText():previous==null?null:previous.getYieldText(),details.heroMediaId()!=null?details.heroMediaId():previous==null?null:previous.getHeroMediaId(),details.videoMediaId()!=null?details.videoMediaId():previous==null?null:previous.getVideoMediaId(),youtube,details.preparationAhead()!=null?details.preparationAhead():previous==null?null:previous.getPreparationAhead(),details.plating()!=null?details.plating():previous==null?null:previous.getPlating(),details.storage()!=null?details.storage():previous==null?null:previous.getStorage(),details.reheating()!=null?details.reheating():previous==null?null:previous.getReheating(),details.tips()!=null?details.tips():previous==null?null:previous.getTips(),details.variants()!=null?details.variants():previous==null?null:previous.getVariants(),details.accompaniments()!=null?details.accompaniments():previous==null?null:previous.getAccompaniments(),details.safetyNotes()!=null?details.safetyNotes():previous==null?null:previous.getSafetyNotes(),equipment,gallery);
            content.synchronizeRecipe(recipe);
            return;
        }
        content.replaceDetails(null, null);
    }

    private Map<String, Object> contentEventPayload(CultureContent content, String title, boolean active, String actor) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("contentId", content.getId());
        payload.put("type", content.getType().name());
        payload.put("title", title);
        payload.put("isActive", active);
        payload.put("status", content.getStatus().name());
        payload.put("countryCode", content.getCountryCode());
        payload.put("contributorId", actor);
        return payload;
    }

    private boolean active(CultureContent content) {
        return content.getStatus() == ContentStatus.PUBLISHED && content.getVisibility() == Visibility.PUBLIC
                && content.getSensitivityLevel() != SensitivityLevel.SACRED && content.getSensitivityLevel() != SensitivityLevel.COMMUNITY_RESTRICTED;
    }

    private String title(CultureContent content) {
        return translations.findByContentIdOrderByLanguageCode(content.getId()).stream().findFirst().map(CultureTranslation::getTitle).orElse(content.getSlug());
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
