package com.yeyamo_mobile.api.event_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.event_service.dto.EventSummaryResponse;
import com.yeyamo_mobile.api.event_service.service.EventService;

@RestController
@RequestMapping("/api/v1/places")
@Validated
public class PlaceEventController {

    private final EventService eventService;

    public PlaceEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{placeId}/events")
    public List<EventSummaryResponse> findByPlace(
            @PathVariable UUID placeId,
            @RequestParam(defaultValue = "true") boolean publishedOnly
    ) {
        return eventService.findByPlaceId(placeId, publishedOnly);
    }
}
