package com.travelplanningplatform.service;

import com.travelplanningplatform.client.AmadeusClient;
import com.travelplanningplatform.dto.external.FlightSearchRequest;
import com.travelplanningplatform.dto.external.FlightSearchResponse;
import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FlightService {

    private final AmadeusClient amadeusClient;
    private final TripService tripService;
    private final AirportDataService airportDataService;


    public FlightService(AmadeusClient amadeusClient,
                        @Autowired(required = false) TripService tripService,
                        AirportDataService airportDataService) {
        this.amadeusClient = amadeusClient;
        this.tripService = tripService;
        this.airportDataService = airportDataService;

        System.out.println("✅ FlightService: Initialized with direct airport code support");
    }

    public Mono<FlightSearchResponse> searchFlights(FlightSearchRequest request, boolean directFlightsOnly) {
        FlightSearchRequest modifiedRequest = directFlightsOnly ?
            new FlightSearchRequest(
                request.originLocationCode(),
                request.destinationLocationCode(),
                request.departureDate(),
                request.returnDate(),
                request.adults(),
                request.children(),
                request.infants(),
                request.travelClass(),
                true,
                request.currencyCode(),
                request.max()
            ) : request;

        System.out.println("🚀 FlightService: Delegating to AmadeusClient for flight search" +
                          (directFlightsOnly ? " (direct flights only)" : ""));

        return amadeusClient.searchFlights(modifiedRequest)
            .doOnSuccess(response -> System.out.println("FlightService: Received response from AmadeusClient"))
            .doOnError(error -> System.err.println("FlightService: Error from AmadeusClient: " + error.getMessage()));
    }


    public Mono<FlightSearchResponse> searchFlightsForTripWithAirportCodes(Long tripId, String originAirport,
                                                                           String destinationAirport, Boolean directFlightsOnly, Long userId) {
        System.out.println("✈️ FlightService: Flight search for trip " + tripId + " using airport codes: " + originAirport + " → " +
                          (destinationAirport != null ? destinationAirport : "from trip destination") +
                          (Boolean.TRUE.equals(directFlightsOnly) ? " (direct flights only)" : ""));

        try {
            Trip trip = tripService.getTripByIdAndUser(tripId, userId);
            System.out.println("FlightService: Found trip - " + trip.getTitle() + " to " + trip.getDestination());

            // Use destination airport code or default to a common airport if not provided
            String destCode = destinationAirport != null ? destinationAirport : "CDG"; // Default fallback

            FlightSearchRequest request = new FlightSearchRequest(
                originAirport,
                destCode,
                trip.getStartDate(),
                null, // returnDate
                trip.getTravelersCount(),
                0, // children
                0, // infants
                "ECONOMY",
                Boolean.TRUE.equals(directFlightsOnly), // nonStop
                "USD",
                10 // max
            );

            System.out.println("✈️ FlightService: Making flight search: " + originAirport + " → " + destCode +
                             " on " + trip.getStartDate() + " for " + trip.getTravelersCount() + " passengers" +
                             (Boolean.TRUE.equals(directFlightsOnly) ? " (direct flights only)" : ""));

            return searchFlights(request, directFlightsOnly)
                .doOnSuccess(response -> System.out.println("FlightService: Flight search completed successfully"));

        } catch (Exception e) {
            System.err.println("FlightService: Exception in searchFlightsForTripWithAirportCodes: " + e.getMessage());
            e.printStackTrace();
            return Mono.just(new FlightSearchResponse());
        }
    }

    public AirportDataService.AirportInfo getAirportInfo(String iataCode) {
        return airportDataService.getAirportInfo(iataCode).orElse(null);
    }

    public List<AirportDataService.AirportInfo> searchAirports(String partialName) {
        return airportDataService.searchAirportsByName(partialName);
    }

    public List<AirportDataService.AirportInfo> getAirportsByCountry(String country) {
        return airportDataService.getAirportsByCountry(country);
    }

    public Map<String, Object> getSupportedCities() {
        // Return airport statistics instead of city mappings
        return airportDataService.getStats();
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>(airportDataService.getStats());
        stats.put("flightService", "Amadeus API integration");
        stats.put("dataReady", airportDataService.isDataReady());
        return stats;
    }

    /**
     * Resolves either an airport code (like "IST") or city name (like "Istanbul") to an IATA code
     * @param input Either a 3-letter IATA code or a city name
     * @return The IATA code, or null if not found
     */
    public String resolveToAirportCode(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new BadRequestException("Airport code or city name is required");
        }

        String trimmedInput = input.trim();
        System.out.println("🔍 FlightService: Resolving '" + trimmedInput + "'...");

        if (trimmedInput.length() == 3 && trimmedInput.matches("[A-Za-z]{3}")) {
            String iataCode = trimmedInput.toUpperCase();
            if (airportDataService.getAirportInfo(iataCode).isPresent()) {
                System.out.println("FlightService: Resolved airport code " + iataCode);
                return iataCode;
            } else {
                System.out.println("FlightService: Airport code " + iataCode + " not found in database");
                throw new BadRequestException("Airport code '" + iataCode + "' not found");
            }
        }

        // Check if airport data is ready
        if (!airportDataService.isDataReady()) {
            System.err.println("FlightService: Airport data not yet loaded.");
            throw new BadRequestException("Airport data is not yet available. Please try again later");
        }

        // Search for airports by city name
        System.out.println("🔍 FlightService: Searching airports for city: " + trimmedInput);
        List<AirportDataService.AirportInfo> airports = airportDataService.searchAirportsByName(trimmedInput);

        System.out.println("🔍 FlightService: Found " + airports.size() + " airports for '" + trimmedInput + "'");

        if (!airports.isEmpty()) {
            // Return the first (most relevant) airport for the city
            AirportDataService.AirportInfo primaryAirport = airports.getFirst();
            System.out.println("FlightService: Resolved city '" + trimmedInput + "' to airport " +
                             primaryAirport.iataCode() + " (" + primaryAirport.name() + " in " + primaryAirport.city() + ")");
            return primaryAirport.iataCode();
        }

        throw new BadRequestException("Could not resolve '" + trimmedInput + "' to any known airport");
    }
}

