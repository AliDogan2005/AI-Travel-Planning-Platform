package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.ItineraryCreateRequest;
import com.travelplanningplatform.dto.ItineraryGenerateRequest;
import com.travelplanningplatform.entity.Itinerary;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.service.ItineraryGenerationService;
import com.travelplanningplatform.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final ItineraryGenerationService itineraryGenerationService;

    public ItineraryController(ItineraryService itineraryService, ItineraryGenerationService itineraryGenerationService) {
        this.itineraryService = itineraryService;
        this.itineraryGenerationService = itineraryGenerationService;
    }

    @PostMapping("/trips/{tripId}/itineraries")
    public ResponseEntity<Itinerary> createItinerary(@Valid @RequestBody ItineraryCreateRequest dto,
                                                     @PathVariable Long tripId,
                                                     @AuthenticationPrincipal User user) {
        Itinerary itinerary = itineraryService.createItinerary(dto, tripId, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(itinerary);
    }

    @GetMapping("/trips/{tripId}/itineraries")
    public ResponseEntity<List<Itinerary>> getTripItineraries(@PathVariable Long tripId,
                                                              @AuthenticationPrincipal User user){
        return ResponseEntity.ok(itineraryService.getItinerariesByTrip(tripId , user.getId()));
    }

    @GetMapping("/trips/{tripId}/itineraries/day/{dayNumber}")
    public ResponseEntity<Itinerary> getItineraryByDay(@PathVariable Long tripId,
                                                       @PathVariable Integer dayNumber,
                                                       @AuthenticationPrincipal User user){
        return ResponseEntity.ok(itineraryService.getItineraryByTripAndDay(tripId , dayNumber , user.getId()));
    }

    @GetMapping("/trips/{tripId}/itineraries/dates")
    public ResponseEntity<List<Itinerary>> getItineraryByDateRange(@PathVariable Long tripId,
                                                                   @RequestParam LocalDate startDate,
                                                                   @RequestParam LocalDate endDate,
                                                                   @AuthenticationPrincipal User user){
        return ResponseEntity.ok(itineraryService.getItinerariesByDateRange(tripId , startDate , endDate , user.getId()));
    }

    @GetMapping("/trips/{tripId}/itineraries/ai-generated")
    public ResponseEntity<List<Itinerary>> getAiGenerated(@PathVariable Long tripId,
                                                          @AuthenticationPrincipal User user){
        return ResponseEntity.ok(itineraryService.getAiGeneratedItineraries(tripId , user.getId()));
    }

    @PostMapping("/trips/{tripId}/itineraries/generate")
    public ResponseEntity<List<Itinerary>> generateItinerary(
            @Valid @RequestBody ItineraryGenerateRequest request,
            @PathVariable Long tripId,
            @AuthenticationPrincipal User user) {

        if (!tripId.equals(request.tripId())) {
            throw new IllegalArgumentException("Trip ID in path does not match request body");
        }

        List<Itinerary> itineraries = itineraryGenerationService.generateItinerary(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(itineraries);
    }

    @DeleteMapping("/itineraries/{itineraryId}")
    public ResponseEntity<Void> deleteItinerary(@PathVariable Long itineraryId , @AuthenticationPrincipal User user){
        itineraryService.deleteItinerary(itineraryId , user.getId());

        return ResponseEntity.noContent().build();
    }
}
