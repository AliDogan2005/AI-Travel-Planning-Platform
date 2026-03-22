package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.TripCreateRequest;
import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.entity.enums.TripStatus;
import com.travelplanningplatform.exception.UnauthorizedException;
import com.travelplanningplatform.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService){
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(
            @Valid @RequestBody TripCreateRequest dto,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        Trip trip = tripService.createTrip(dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(trip);
    }

    @GetMapping
    public ResponseEntity<List<Trip>> getUserTrips(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        List<Trip> trips = tripService.getTripsByUser(user.getId());
        System.out.println("DEBUG: Found " + trips.size() + " trips for user " + user.getUsername());
        return ResponseEntity.ok(trips);
    }
    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTripById(
            @PathVariable Long tripId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        Trip trip = tripService.getTripByIdAndUser(tripId, user.getId());
        return ResponseEntity.ok(trip);
    }
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Trip>> getTripsByStatus(
            @PathVariable TripStatus status,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        List<Trip> trips = tripService.getTripsByStatus(user.getId(), status);
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/public")
    public ResponseEntity<List<Trip>> getPublicTrips() {
        List<Trip> trips = tripService.getPublicTrips();
        return ResponseEntity.ok(trips);
    }
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable Long tripId,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        tripService.deleteTrip(tripId, user.getId());
        return ResponseEntity.noContent().build();
    }





}
