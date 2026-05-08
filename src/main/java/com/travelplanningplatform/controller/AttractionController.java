package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.external.PoiSearchRequest;
import com.travelplanningplatform.dto.external.PoiSearchResponse;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.exception.UnauthorizedException;
import com.travelplanningplatform.service.AttractionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attractions")
public class AttractionController {

    private final AttractionService attractionService;

    public AttractionController(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    @GetMapping("/search")
    public ResponseEntity<PoiSearchResponse> searchPois(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude,
        @RequestParam(defaultValue = "5000") Integer radiusMeters,
        @RequestParam(required = false) String categories,
        @RequestParam(defaultValue = "20") Integer limit,
        @AuthenticationPrincipal User user) {

        requireUser(user);

        PoiSearchRequest request = new PoiSearchRequest(
            query,
            latitude,
            longitude,
            radiusMeters,
            categories,
            limit
        );

        PoiSearchResponse response = attractionService.searchPois(request).block();

        if (response == null || !response.hasResults()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<PoiSearchResponse> searchPois(
        @Valid @RequestBody PoiSearchRequest request,
        @AuthenticationPrincipal User user) {

        requireUser(user);

        PoiSearchResponse response = attractionService.searchPois(request).block();

        if (response == null || !response.hasResults()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<PoiSearchResponse> searchPoisForTrip(
        @PathVariable Long tripId,
        @RequestParam(defaultValue = "5000") Integer radiusMeters,
        @RequestParam(required = false) String categories,
        @RequestParam(defaultValue = "20") Integer limit,
        @AuthenticationPrincipal User user) {

        requireUser(user);

        PoiSearchResponse response = attractionService
            .searchPoisForTrip(tripId, user.getId(), radiusMeters, categories, limit)
            .block();

        if (response == null || !response.hasResults()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Attraction service is working");
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
    }
}

