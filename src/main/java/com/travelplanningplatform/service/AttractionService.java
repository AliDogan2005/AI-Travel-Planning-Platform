package com.travelplanningplatform.service;

import com.travelplanningplatform.client.AmadeusPoiClient;
import com.travelplanningplatform.dto.external.PoiSearchRequest;
import com.travelplanningplatform.dto.external.PoiSearchResponse;
import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.exception.BadRequestException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AttractionService {

    private static final int MAX_LIMIT = 100;

    private final AmadeusPoiClient amadeusPoiClient;
    private final TripService tripService;

    public AttractionService(AmadeusPoiClient amadeusPoiClient, TripService tripService) {
        this.amadeusPoiClient = amadeusPoiClient;
        this.tripService = tripService;
    }

    public Mono<PoiSearchResponse> searchPois(PoiSearchRequest request) {
        if ((request.query() == null || request.query().isBlank())
            && (request.latitude() == null || request.longitude() == null)) {
            throw new BadRequestException("Query or coordinates are required");
        }

        if (request.limit() != null && request.limit() > MAX_LIMIT) {
            throw new BadRequestException("Limit cannot be greater than " + MAX_LIMIT);
        }

        return amadeusPoiClient.searchPois(request)
            .onErrorMap(ex -> {
                if (ex instanceof BadRequestException) {
                    return ex;
                }
                return new BadRequestException("POI search failed: " + ex.getMessage());
            });
    }

    public Mono<PoiSearchResponse> searchPoisForTrip(Long tripId, Long userId, Integer radiusMeters, String categories, Integer limit) {
        Trip trip = tripService.getTripByIdAndUser(tripId, userId);

        PoiSearchRequest request = new PoiSearchRequest(
            trip.getDestination(),
            null,
            null,
            radiusMeters,
            categories,
            limit
        );

        return searchPois(request);
    }
}

