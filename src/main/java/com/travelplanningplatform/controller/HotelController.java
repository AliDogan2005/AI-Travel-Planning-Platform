package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.external.HotelSearchRequest;
import com.travelplanningplatform.dto.external.HotelSearchResponse;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.exception.UnauthorizedException;
import com.travelplanningplatform.service.HotelService;
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
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/search")
    public ResponseEntity<HotelSearchResponse> searchHotels(
        @RequestParam String destination,
        @RequestParam(defaultValue = "5000") Integer radiusMeters,
        @RequestParam(defaultValue = "20") Integer maxResults,
        @AuthenticationPrincipal User user) {

        requireUser(user);

        HotelSearchRequest request = new HotelSearchRequest(destination, radiusMeters, maxResults);
        HotelSearchResponse response = hotelService.searchHotels(request).block();

        if (response == null || response.hasResults()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<HotelSearchResponse> searchHotels(
        @Valid @RequestBody HotelSearchRequest request,
        @AuthenticationPrincipal User user) {

        requireUser(user);

        HotelSearchResponse response = hotelService.searchHotels(request).block();

        if (response == null || response.hasResults()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<HotelSearchResponse> searchHotelsForTrip(
        @PathVariable Long tripId,
        @RequestParam(defaultValue = "5000") Integer radiusMeters,
        @RequestParam(defaultValue = "20") Integer maxResults,
        @AuthenticationPrincipal User user) {

        requireUser(user);

        HotelSearchResponse response = hotelService
            .searchHotelsForTrip(tripId, user.getId(), radiusMeters, maxResults)
            .block();

        if (response == null || response.hasResults()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Hotel service is working");
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
    }
}

