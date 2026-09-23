package com.yeyamo_mobile.api.place_service.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.place_service.dto.PlaceRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceResponse;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionDuplicateCheckRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionDuplicateCheckResponse;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionModerationRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionRejectionRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionResponse;
import com.yeyamo_mobile.api.place_service.enums.MediaType;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.event.PlaceEventPublisher;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.Place;
import com.yeyamo_mobile.api.place_service.models.PlaceSuggestion;
import com.yeyamo_mobile.api.place_service.models.PlaceSuggestionMedia;
import com.yeyamo_mobile.api.place_service.repository.PlaceRepository;
import com.yeyamo_mobile.api.place_service.repository.PlaceSuggestionRepository;
import com.yeyamo_mobile.shared.country.CountryConfigClient;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryConfigException;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryFeature;

@Service
@Transactional
public class PlaceSuggestionService {
    private static final Logger log = LoggerFactory.getLogger(PlaceSuggestionService.class);
    private static final double CERTAIN_DUPLICATE_RADIUS_METERS = 100d;
    private static final double POSSIBLE_DUPLICATE_RADIUS_METERS = 250d;

    private final PlaceSuggestionRepository suggestions;
    private final PlaceRepository places;
    private final PlaceService placeService;
    private final CountryConfigClient countries;
    private final SuggestionMediaVerifier mediaVerifier;
    private final PlaceEventPublisher events;
    private final int maxMedia;

    public PlaceSuggestionService(PlaceSuggestionRepository suggestions, PlaceRepository places, PlaceService placeService,
            CountryConfigClient countries, SuggestionMediaVerifier mediaVerifier, PlaceEventPublisher events,
            @Value("${yeyamo.place-suggestions.max-media:8}") int maxMedia) {
        this.suggestions = suggestions;
        this.places = places;
        this.placeService = placeService;
        this.countries = countries;
        this.mediaVerifier = mediaVerifier;
        this.events = events;
        this.maxMedia = Math.max(1, maxMedia);
    }

    public PlaceSuggestionResponse create(PlaceSuggestionRequest request, String actor) {
        String countryCode = validateCountry(request.countryCode());
        validateGeography(countryCode, request.administrativeAreaId(), request.cityId(), request.localityId());
        List<UUID> mediaIds = validatedMediaIds(request.mediaIds());
        List<SuggestionMediaVerifier.VerifiedMedia> media = mediaVerifier.verifyOwnedUsableVisualMedia(mediaIds, actor);
        String name = normalize(request.name());
        String address = normalize(request.address());
        List<PlaceSuggestionDuplicateCheckResponse.Candidate> duplicates = duplicateCandidates(countryCode, name, address,
                request.latitude(), request.longitude());
        if (duplicates.stream().anyMatch(PlaceSuggestionDuplicateCheckResponse.Candidate::certain)) {
            throw duplicateConflict();
        }

        PlaceSuggestion suggestion = PlaceSuggestion.pending(actor, request.name().trim(), name, request.address().trim(),
                address, blankToNull(request.description()), blankToNull(request.category()), blankToNull(request.placeType()),
                blankToNull(request.region()), countryCode, dedupeKey(countryCode, name, address, request.latitude(), request.longitude()),
                request.administrativeAreaId(), request.cityId(), request.localityId(),
                request.latitude(), request.longitude());
        for (int index = 0; index < media.size(); index++) {
            SuggestionMediaVerifier.VerifiedMedia value = media.get(index);
            suggestion.addMedia(value.mediaId(), value.type(), value.contentType(), value.contentUrl(), value.thumbnailUrl(), index);
        }
        try {
            PlaceSuggestion saved = suggestions.saveAndFlush(suggestion);
            events.publishSuggestionCreated(saved);
            log.info("Place suggestion created suggestionId={} userId={} country={} candidates={} mediaCount={}", saved.getId(),
                    actor, countryCode, duplicates.size(), media.size());
            return PlaceSuggestionResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            // V8's partial natural-key index protects the create/preflight race for equivalent pending submissions.
            throw duplicateConflict();
        }
    }

    @Transactional(readOnly = true)
    public PlaceSuggestionDuplicateCheckResponse checkDuplicates(PlaceSuggestionDuplicateCheckRequest request) {
        String countryCode = validateCountry(request.countryCode());
        if (request.cityId() != null) countries.validateCity(countryCode, request.cityId());
        return new PlaceSuggestionDuplicateCheckResponse(duplicateCandidates(countryCode, normalize(request.name()),
                normalize(request.address()), request.latitude(), request.longitude()));
    }

    @Transactional(readOnly = true)
    public Page<PlaceSuggestionResponse> mine(String actor, Pageable pageable) {
        return suggestions.findBySubmitterUserIdOrderByCreatedAtDesc(actor, bounded(pageable)).map(PlaceSuggestionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PlaceSuggestionResponse> moderateList(PlaceSuggestion.Status status, Pageable pageable) {
        if (status == null) return suggestions.findAll(bounded(pageable)).map(PlaceSuggestionResponse::from);
        return suggestions.findByStatusOrderByCreatedAtDesc(status, bounded(pageable)).map(PlaceSuggestionResponse::from);
    }

    public PlaceSuggestionResponse approve(UUID id, PlaceSuggestionModerationRequest request, String moderator) {
        PlaceSuggestion suggestion = locked(id);
        if (suggestion.getStatus() == PlaceSuggestion.Status.APPROVED) return PlaceSuggestionResponse.from(suggestion);
        if (suggestion.getStatus() != PlaceSuggestion.Status.PENDING) throw notPending("approuvee");

        Place existing = matchingCanonical(suggestion).orElse(null);
        UUID canonicalId;
        if (existing != null) {
            canonicalId = existing.getId();
        } else {
            canonicalId = createCanonicalPlace(suggestion, request).id();
            placeService.attachApprovedSuggestionMedia(canonicalId, suggestion.getMedia().stream()
                    .map(media -> new PlaceService.ApprovedMediaReference(media.getMediaId(), media.getContentUrl(),
                            MediaType.valueOf(media.getType()), media.getDisplayOrder()))
                    .toList());
        }
        suggestion.approve(canonicalId, moderator, blankToNull(request.reason()));
        PlaceSuggestion saved = suggestions.save(suggestion);
        events.publishSuggestionApproved(saved);
        log.info("Place suggestion approved suggestionId={} moderatorId={} canonicalPlaceId={} createdCanonical={}",
                saved.getId(), moderator, canonicalId, existing == null);
        return PlaceSuggestionResponse.from(saved);
    }

    public PlaceSuggestionResponse reject(UUID id, PlaceSuggestionRejectionRequest request, String moderator) {
        PlaceSuggestion suggestion = locked(id);
        if (suggestion.getStatus() == PlaceSuggestion.Status.REJECTED) return PlaceSuggestionResponse.from(suggestion);
        if (suggestion.getStatus() != PlaceSuggestion.Status.PENDING) throw notPending("rejetee");
        suggestion.reject(moderator, request.reason().trim());
        PlaceSuggestion saved = suggestions.save(suggestion);
        events.publishSuggestionRejected(saved);
        log.info("Place suggestion rejected suggestionId={} moderatorId={}", saved.getId(), moderator);
        return PlaceSuggestionResponse.from(saved);
    }

    private PlaceSuggestion locked(UUID id) {
        return suggestions.findLockedById(id).orElseThrow(
                () -> new ApiException("SUGGESTION_NOT_FOUND", "Suggestion introuvable", HttpStatus.NOT_FOUND));
    }

    private ApiException notPending(String action) {
        return new ApiException("INVALID_SUGGESTION_STATUS", "Cette suggestion ne peut plus etre " + action,
                HttpStatus.CONFLICT);
    }

    private PlaceResponse createCanonicalPlace(PlaceSuggestion suggestion, PlaceSuggestionModerationRequest request) {
        PlaceRequest place = new PlaceRequest();
        place.setCategoryId(request.categoryId());
        place.setRegionId(request.regionId());
        place.setCityId(request.cityId());
        place.setDistrictId(request.districtId());
        place.setName(suggestion.getName());
        place.setSlug(request.slug());
        place.setDescription(suggestion.getDescription());
        place.setLatitude(suggestion.getLatitude());
        place.setLongitude(suggestion.getLongitude());
        place.setAddress(suggestion.getAddress());
        place.setCountryCode(suggestion.getCountryCode());
        place.setStatus(request.effectivePlaceStatus());
        return placeService.create(place);
    }

    private java.util.Optional<Place> matchingCanonical(PlaceSuggestion suggestion) {
        return places.findNearbyDuplicateCandidates(suggestion.getLatitude(), suggestion.getLongitude(),
                        POSSIBLE_DUPLICATE_RADIUS_METERS).stream()
                .filter(value -> certain(value.getName(), value.getAddress(), value.getLatitude(), value.getLongitude(),
                        suggestion.getNormalizedName(), suggestion.getNormalizedAddress(), suggestion.getLatitude(),
                        suggestion.getLongitude()))
                .findFirst();
    }

    private List<PlaceSuggestionDuplicateCheckResponse.Candidate> duplicateCandidates(String countryCode,
            String normalizedName, String normalizedAddress, double latitude, double longitude) {
        List<PlaceSuggestionDuplicateCheckResponse.Candidate> candidates = new ArrayList<>();
        for (Place place : places.findNearbyDuplicateCandidates(latitude, longitude, POSSIBLE_DUPLICATE_RADIUS_METERS)) {
            boolean sameName = normalize(place.getName()).equals(normalizedName);
            boolean sameAddress = normalize(place.getAddress()).equals(normalizedAddress);
            if (!sameName && !sameAddress) continue;
            double distance = distanceMeters(place.getLatitude(), place.getLongitude(), latitude, longitude);
            candidates.add(new PlaceSuggestionDuplicateCheckResponse.Candidate("CANONICAL_PLACE", place.getId(),
                    place.getName(), place.getAddress(), distance,
                    sameName && sameAddress && distance <= CERTAIN_DUPLICATE_RADIUS_METERS));
        }
        for (PlaceSuggestion suggestion : suggestions.findDuplicateCandidates(PlaceSuggestion.Status.PENDING, countryCode,
                normalizedName, normalizedAddress)) {
            double distance = distanceMeters(suggestion.getLatitude(), suggestion.getLongitude(), latitude, longitude);
            if (distance > POSSIBLE_DUPLICATE_RADIUS_METERS) continue;
            boolean sameName = suggestion.getNormalizedName().equals(normalizedName);
            boolean sameAddress = suggestion.getNormalizedAddress().equals(normalizedAddress);
            candidates.add(new PlaceSuggestionDuplicateCheckResponse.Candidate("PENDING_SUGGESTION", suggestion.getId(),
                    suggestion.getName(), suggestion.getAddress(), distance,
                    sameName && sameAddress && distance <= CERTAIN_DUPLICATE_RADIUS_METERS));
        }
        return List.copyOf(candidates);
    }

    private boolean certain(String name, String address, double latitude, double longitude, String normalizedName,
            String normalizedAddress, double targetLatitude, double targetLongitude) {
        return normalize(name).equals(normalizedName) && normalize(address).equals(normalizedAddress)
                && distanceMeters(latitude, longitude, targetLatitude, targetLongitude) <= CERTAIN_DUPLICATE_RADIUS_METERS;
    }

    private String validateCountry(String value) {
        String countryCode = value.trim().toUpperCase(Locale.ROOT);
        try {
            countries.validateFeature(countryCode, CountryFeature.PLACE_PUBLISHING);
            return countryCode;
        } catch (CountryConfigException exception) {
            HttpStatus status = exception.getCause() == null ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
            String code = status == HttpStatus.BAD_REQUEST ? "INVALID_COUNTRY" : "COUNTRY_VALIDATION_UNAVAILABLE";
            throw new ApiException(code, exception.getMessage(), status);
        } catch (RuntimeException exception) {
            throw new ApiException("COUNTRY_VALIDATION_UNAVAILABLE",
                    "Validation du pays temporairement indisponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void validateGeography(String countryCode, UUID administrativeAreaId, UUID cityId, UUID localityId) {
        try {
            countries.validateAdministrativeArea(countryCode, administrativeAreaId);
            countries.validateCity(countryCode, cityId, administrativeAreaId);
            countries.validateLocality(countryCode, localityId, cityId, administrativeAreaId);
        } catch (CountryConfigException exception) {
            throw new ApiException("INVALID_LOCATION", exception.getMessage(),
                    exception.getCause() == null ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            throw new ApiException("LOCATION_VALIDATION_UNAVAILABLE",
                    "Validation de la localisation temporairement indisponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private List<UUID> validatedMediaIds(List<UUID> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) return List.of();
        if (mediaIds.size() > maxMedia) {
            throw new ApiException("MEDIA_LIMIT_EXCEEDED", "Le nombre maximum de medias est " + maxMedia,
                    HttpStatus.BAD_REQUEST);
        }
        Set<UUID> unique = new HashSet<>(mediaIds);
        if (unique.size() != mediaIds.size() || unique.contains(null)) {
            throw new ApiException("DUPLICATE_MEDIA_ID", "Chaque media ne peut etre ajoute qu'une seule fois",
                    HttpStatus.BAD_REQUEST);
        }
        return List.copyOf(mediaIds);
    }

    private Pageable bounded(Pageable pageable) {
        return org.springframework.data.domain.PageRequest.of(Math.max(0, pageable.getPageNumber()),
                Math.min(100, Math.max(1, pageable.getPageSize())), pageable.getSort());
    }

    private ApiException duplicateConflict() {
        return new ApiException("CERTAIN_DUPLICATE", "Un lieu identique existe deja ou est deja en attente",
                HttpStatus.CONFLICT);
    }

    private String dedupeKey(String countryCode, String normalizedName, String normalizedAddress, double latitude,
            double longitude) {
        return String.format(Locale.ROOT, "%s|%s|%s|%.3f|%.3f", countryCode, normalizedName, normalizedAddress,
                latitude, longitude);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private static double distanceMeters(double firstLatitude, double firstLongitude, double secondLatitude,
            double secondLongitude) {
        double latitudeDelta = Math.toRadians(secondLatitude - firstLatitude);
        double longitudeDelta = Math.toRadians(secondLongitude - firstLongitude);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(firstLatitude)) * Math.cos(Math.toRadians(secondLatitude))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6_371_000d * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    }
}
