package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.external.FlightSearchRequest;
import com.travelplanningplatform.dto.external.FlightSearchResponse;
import com.travelplanningplatform.entity.User;
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
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(defaultValue = "1") Integer passengers,
            @RequestParam(defaultValue = "false") Boolean directFlightsOnly,
            @AuthenticationPrincipal User user) {

        try {
            System.out.println("✈️ FlightController: Searching flights " + origin + " → " + destination +
                             (Boolean.TRUE.equals(directFlightsOnly) ? " (direct flights only)" : ""));

            // Resolve origin and destination to airport codes
            String originAirportCode = flightService.resolveToAirportCode(origin);
            String destinationAirportCode = flightService.resolveToAirportCode(destination);

            if (originAirportCode == null) {
                System.err.println("❌ FlightController: Could not resolve origin: " + origin);
                return ResponseEntity.badRequest().build();
            }

            if (destinationAirportCode == null) {
                System.err.println("❌ FlightController: Could not resolve destination: " + destination);
                return ResponseEntity.badRequest().build();
            }

            System.out.println("✅ FlightController: Resolved " + origin + " → " + originAirportCode +
                             ", " + destination + " → " + destinationAirportCode);

            // Create request with resolved airport codes
            FlightSearchRequest request = new FlightSearchRequest(
                originAirportCode,
                destinationAirportCode,
                departureDate,
                null, // returnDate
                passengers,
                0, // children
                0, // infants
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
            System.err.println("❌ FlightController: Exception in searchFlights: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<FlightSearchResponse> searchFlightsForTrip(
            @PathVariable Long tripId,
            @RequestParam String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "false") Boolean directFlightsOnly,
            @AuthenticationPrincipal User user) {

        System.out.println("✈️ FlightController: Trip flight search - Trip: " + tripId +
                          ", Origin: " + origin +
                          (destination != null ? ", Destination: " + destination : "") +
                          (Boolean.TRUE.equals(directFlightsOnly) ? " (direct flights only)" : ""));

        if (user == null) {
            System.err.println("DEBUG: User is null - authentication failed");
            return ResponseEntity.status(401).build();
        }

        System.out.println("DEBUG: User authenticated: " + user.getUsername() + ", userId: " + user.getId());

        try {
            // Resolve origin to airport code
            String originAirportCode = flightService.resolveToAirportCode(origin);
            if (originAirportCode == null) {
                System.err.println("❌ FlightController: Could not resolve origin: " + origin);
                return ResponseEntity.badRequest().build();
            }

            // Resolve destination to airport code if provided
            String destinationAirportCode = null;
            if (destination != null) {
                destinationAirportCode = flightService.resolveToAirportCode(destination);
                if (destinationAirportCode == null) {
                    System.err.println("❌ FlightController: Could not resolve destination: " + destination);
                    return ResponseEntity.badRequest().build();
                }
            }

            System.out.println("✅ FlightController: Resolved origin " + origin + " → " + originAirportCode +
                             (destinationAirportCode != null ? ", destination " + destination + " → " + destinationAirportCode : ""));

            FlightSearchResponse response = flightService.searchFlightsForTripWithAirportCodes(
                tripId,
                originAirportCode,
                destinationAirportCode,
                directFlightsOnly,
                user.getId()
            ).block();

            System.out.println("DEBUG: FlightService returned response: " + (response != null ? "Success" : "Null"));

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            System.err.println("DEBUG: Exception in searchFlightsForTrip: " + e.getMessage());
            e.printStackTrace();
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

    @GetMapping("/airports/{iataCode}")
    public ResponseEntity<Object> getAirportInfo(@PathVariable String iataCode) {
        var airportInfo = flightService.getAirportInfo(iataCode);
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
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(flightService.getCacheStats());
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Flight service is working! ✈️");
    }
}

