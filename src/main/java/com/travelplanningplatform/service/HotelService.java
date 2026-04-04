package com.travelplanningplatform.service;

import com.travelplanningplatform.client.AmadeusHotelClient;
import com.travelplanningplatform.dto.external.HotelSearchRequest;
import com.travelplanningplatform.dto.external.HotelSearchResponse;
import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.exception.BadRequestException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class HotelService {

    private final AmadeusHotelClient amadeusHotelClient;
    private final TripService tripService;

    public HotelService(AmadeusHotelClient amadeusHotelClient, TripService tripService) {
        this.amadeusHotelClient = amadeusHotelClient;
        this.tripService = tripService;
    }

    public Mono<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        if (request.destination() == null || request.destination().isBlank()) {
            throw new BadRequestException("Destination is required");
        }

        if (request.maxResults() != null && request.maxResults() > 100) {
            throw new BadRequestException("Max results cannot be greater than 100");
        }

        return amadeusHotelClient.searchHotels(request)
            .onErrorMap(ex -> {
                if (ex instanceof BadRequestException) {
                    return ex;
                }
                return new BadRequestException("Hotel search failed: " + ex.getMessage());
            });
    }

    public Mono<HotelSearchResponse> searchHotelsForTrip(Long tripId, Long userId, Integer radiusMeters, Integer maxResults) {
        Trip trip = tripService.getTripByIdAndUser(tripId, userId);

        HotelSearchRequest request = new HotelSearchRequest(
            trip.getDestination(),
            radiusMeters,
            maxResults
        );

        return searchHotels(request);
    }
}

