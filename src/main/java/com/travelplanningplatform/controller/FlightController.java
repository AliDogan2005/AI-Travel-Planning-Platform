package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.external.FlightSearchRequest;
import com.travelplanningplatform.dto.external.FlightSearchResponse;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.exception.UnauthorizedException;
import com.travelplanningplatform.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping("/search")
    public ResponseEntity<FlightSearchResponse> searchFlights(
            @Valid @RequestBody FlightSearchRequest request,
            @RequestParam(defaultValue = "false") Boolean directFlightsOnly,
            @AuthenticationPrincipal User user) {

        try {
            FlightSearchResponse response = flightService.searchFlights(request, directFlightsOnly).block();
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<FlightSearchResponse> searchFlights(
            @RequestParam String originCode,
            @RequestParam String destinationCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(defaultValue = "1") Integer passengers,
            @RequestParam(defaultValue = "false") Boolean directFlightsOnly) {

        try {
            if (originCode == null) {
                return ResponseEntity.badRequest().build();
            }

            if (destinationCode == null) {
                return ResponseEntity.badRequest().build();
            }

            // Create request with resolved airport codes
            FlightSearchRequest request = new FlightSearchRequest(
                originCode,
                destinationCode,
                departureDate,
                null,
                passengers,
                0,
                0,
                "ECONOMY",
                directFlightsOnly, // nonStop
                "USD",
                10 // max
            );

            FlightSearchResponse response = flightService.searchFlights(request, directFlightsOnly).block();

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<FlightSearchResponse> searchFlightsForTrip(
            @PathVariable Long tripId,
            @RequestParam String originCode,
            @RequestParam(required = false) String destinationCode,
            @RequestParam(defaultValue = "false") Boolean directFlightsOnly,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }

        try {
            // Resolve origin to airport code
            if (originCode == null) {
                return ResponseEntity.badRequest().build();
            }

            FlightSearchResponse response = flightService.searchFlightsForTripWithAirportCodes(
                tripId,
                originCode,
                destinationCode,
                directFlightsOnly,
                user.getId()
            ).block();

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/airports/supported")
    public ResponseEntity<Map<String, Object>> getSupportedCities() {
        return ResponseEntity.ok(flightService.getSupportedCities());
    }

    @GetMapping("/airports/search")
    public ResponseEntity<List<Object>> searchAirports(@RequestParam String query) {
        List<Object> results = flightService.searchAirports(query)
            .stream()
            .map(airport -> Map.of(
                "iataCode", airport.iataCode(),
                "name", airport.name(),
                "city", airport.city(),
                "country", airport.country()
            ))
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/airports/{code}")
    public ResponseEntity<Object> getAirportInfo(@PathVariable String code) {
        var airportInfo = flightService.getAirportInfo(code);
        if (airportInfo != null) {
            return ResponseEntity.ok(airportInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/airports/country/{country}")
    public ResponseEntity<List<Object>> getAirportsByCountry(@PathVariable String country) {
        List<Object> results = flightService.getAirportsByCountry(country)
            .stream()
            .limit(20)
            .map(airport -> Map.of(
                "iataCode", airport.iataCode(),
                "name", airport.name(),
                "city", airport.city()
            ))
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/airports/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }

        return ResponseEntity.ok(flightService.getCacheStats());
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Flight service is working! ✈️");
    }

    @GetMapping("/airports/countries/all")
    public ResponseEntity<List<String>> getAllCountries() {
        List<String> countries = flightService.getAllCountries();
        return ResponseEntity.ok(countries);
    }
}

