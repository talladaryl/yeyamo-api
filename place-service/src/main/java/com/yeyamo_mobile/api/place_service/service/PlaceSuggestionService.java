package com.yeyamo_mobile.api.place_service.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.place_service.dto.PlaceRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceResponse;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionModerationRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionRejectionRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceSuggestionResponse;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.Place;
import com.yeyamo_mobile.api.place_service.models.PlaceSuggestion;
import com.yeyamo_mobile.api.place_service.repository.PlaceRepository;
import com.yeyamo_mobile.api.place_service.repository.PlaceSuggestionRepository;

@Service
@Transactional
public class PlaceSuggestionService {
    private static final double DUPLICATE_RADIUS_METERS = 100d;

    private final PlaceSuggestionRepository suggestions;
    private final PlaceRepository places;
    private final PlaceService placeService;

    public PlaceSuggestionService(PlaceSuggestionRepository suggestions, PlaceRepository places, PlaceService placeService) {
        this.suggestions = suggestions;
        this.places = places;
        this.placeService = placeService;
    }

    public PlaceSuggestionResponse create(PlaceSuggestionRequest request, String actor) {
        String name = normalize(request.name());
        String address = normalize(request.address());
        if (hasCanonicalDuplicate(name, address, request.latitude(), request.longitude())
                || suggestions.findDuplicateCandidates(PlaceSuggestion.Status.PENDING, name, address).stream()
                        .anyMatch(value -> sameLocation(value.getLatitude(), value.getLongitude(), request.latitude(), request.longitude()))) {
            throw new ApiException("PLACE_SUGGESTION_DUPLICATE", "Un lieu similaire est deja en attente ou existe deja", HttpStatus.CONFLICT);
        }
        PlaceSuggestion suggestion = PlaceSuggestion.pending(actor, request.name().trim(), name, request.address().trim(), address,
                blankToNull(request.description()), blankToNull(request.category()), blankToNull(request.placeType()),
                blankToNull(request.region()), blankToNull(request.countryCode()), request.latitude(), request.longitude());
        return PlaceSuggestionResponse.from(suggestions.save(suggestion));
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
        PlaceSuggestion suggestion = suggestions.findLockedById(id)
                .orElseThrow(() -> new ApiException("PLACE_SUGGESTION_NOT_FOUND", "Suggestion introuvable", HttpStatus.NOT_FOUND));
        if (suggestion.getStatus() == PlaceSuggestion.Status.APPROVED) return PlaceSuggestionResponse.from(suggestion);
        if (suggestion.getStatus() != PlaceSuggestion.Status.PENDING) {
            throw new ApiException("PLACE_SUGGESTION_NOT_PENDING", "Cette suggestion ne peut plus etre approuvee", HttpStatus.CONFLICT);
        }

        UUID canonicalId = matchingCanonical(suggestion).map(Place::getId).orElseGet(() -> createCanonicalPlace(suggestion, request).id());
        suggestion.approve(canonicalId, moderator, blankToNull(request.reason()));
        return PlaceSuggestionResponse.from(suggestions.save(suggestion));
    }

    public PlaceSuggestionResponse reject(UUID id, PlaceSuggestionRejectionRequest request, String moderator) {
        PlaceSuggestion suggestion = suggestions.findLockedById(id)
                .orElseThrow(() -> new ApiException("PLACE_SUGGESTION_NOT_FOUND", "Suggestion introuvable", HttpStatus.NOT_FOUND));
        if (suggestion.getStatus() == PlaceSuggestion.Status.REJECTED) return PlaceSuggestionResponse.from(suggestion);
        if (suggestion.getStatus() != PlaceSuggestion.Status.PENDING) {
            throw new ApiException("PLACE_SUGGESTION_NOT_PENDING", "Cette suggestion ne peut plus etre rejetee", HttpStatus.CONFLICT);
        }
        suggestion.reject(moderator, request.reason().trim());
        return PlaceSuggestionResponse.from(suggestions.save(suggestion));
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
        place.setStatus(request.effectivePlaceStatus());
        return placeService.create(place);
    }

    private boolean hasCanonicalDuplicate(String name, String address, double latitude, double longitude) {
        return places.findDuplicateCandidates(name, address).stream()
                .anyMatch(value -> normalize(value.getName()).equals(name)
                        && normalize(value.getAddress()).equals(address)
                        && sameLocation(value.getLatitude(), value.getLongitude(), latitude, longitude));
    }

    private java.util.Optional<Place> matchingCanonical(PlaceSuggestion suggestion) {
        return places.findDuplicateCandidates(suggestion.getNormalizedName(), suggestion.getNormalizedAddress()).stream()
                .filter(value -> normalize(value.getName()).equals(suggestion.getNormalizedName())
                        && normalize(value.getAddress()).equals(suggestion.getNormalizedAddress())
                        && sameLocation(value.getLatitude(), value.getLongitude(), suggestion.getLatitude(), suggestion.getLongitude()))
                .findFirst();
    }

    private Pageable bounded(Pageable pageable) {
        return org.springframework.data.domain.PageRequest.of(Math.max(0, pageable.getPageNumber()), Math.min(100, Math.max(1, pageable.getPageSize())), pageable.getSort());
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private static boolean sameLocation(double firstLatitude, double firstLongitude, double secondLatitude, double secondLongitude) {
        double latitudeDelta = Math.toRadians(secondLatitude - firstLatitude);
        double longitudeDelta = Math.toRadians(secondLongitude - firstLongitude);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(firstLatitude)) * Math.cos(Math.toRadians(secondLatitude))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6_371_000d * 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a)) <= DUPLICATE_RADIUS_METERS;
    }
}
