package com.travelplanningplatform.service;

import com.travelplanningplatform.client.AmadeusFlightClient;
import com.travelplanningplatform.dto.external.FlightSearchRequest;
import com.travelplanningplatform.dto.external.FlightSearchResponse;
import com.travelplanningplatform.entity.Trip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FlightService {

    private final AmadeusFlightClient amadeusFlightClient;
    private final TripService tripService;
    private final AirportDataService airportDataService;


    public FlightService(AmadeusFlightClient amadeusFlightClient,
                         @Autowired(required = false) TripService tripService,
                         AirportDataService airportDataService) {
        this.amadeusFlightClient = amadeusFlightClient;
        this.tripService = tripService;
        this.airportDataService = airportDataService;
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

        return amadeusFlightClient.searchFlights(modifiedRequest);
    }


    public Mono<FlightSearchResponse> searchFlightsForTripWithAirportCodes(Long tripId, String originCode,
                                                                           String destinationCode, Boolean directFlightsOnly, Long userId) {
        try {
            Trip trip = tripService.getTripByIdAndUser(tripId, userId);

            FlightSearchRequest request = new FlightSearchRequest(
                originCode,
                destinationCode,
                trip.getStartDate(),
                null,
                trip.getTravelersCount(),
                0,
                0,
                "ECONOMY",
                Boolean.TRUE.equals(directFlightsOnly),
                "USD",
                10
            );

            return searchFlights(request, directFlightsOnly);

        } catch (Exception e) {
            return Mono.just(new FlightSearchResponse());
        }
    }

    public AirportDataService.AirportInfo getAirportInfo(String code) {
        return airportDataService.getAirportInfo(code).orElse(null);
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

    public List<String> getAllCountries() {
        return airportDataService.getAllCountries();
    }
}

