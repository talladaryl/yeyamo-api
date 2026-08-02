package com.yeyamo_mobile.api.ads_delivery_service.interfaces.rest;

import com.yeyamo_mobile.api.ads_delivery_service.application.AdDeliveryService;
import com.yeyamo_mobile.api.ads_delivery_service.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ads")
@Tag(name = "Ad Delivery", description = "Ad selection and tracking endpoints")
public class AdDeliveryController {
    
    private final AdDeliveryService adDeliveryService;

    public AdDeliveryController(AdDeliveryService adDeliveryService) {
        this.adDeliveryService = adDeliveryService;
    }

    @PostMapping("/select")
    @Operation(summary = "Select sponsored placements for a context")
    public ResponseEntity<List<SponsoredPlacementResponse>> selectAds(@Valid @RequestBody AdSelectionRequest request) {
        List<SponsoredPlacementResponse> placements = adDeliveryService.selectAds(request);
        return ResponseEntity.ok(placements);
    }

    @PostMapping("/impressions")
    @Operation(summary = "Record ad impression")
    public ResponseEntity<Void> recordImpression(@Valid @RequestBody ImpressionRequest request) {
        adDeliveryService.recordImpression(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/clicks")
    @Operation(summary = "Record ad click")
    public ResponseEntity<Void> recordClick(@Valid @RequestBody ClickRequest request) {
        adDeliveryService.recordClick(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/conversions")
    @Operation(summary = "Record ad conversion")
    public ResponseEntity<Void> recordConversion(@Valid @RequestBody ConversionRequest request) {
        adDeliveryService.recordConversion(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
