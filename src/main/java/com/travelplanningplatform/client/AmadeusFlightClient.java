package com.travelplanningplatform.client;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.FlightOfferSearch;
import com.travelplanningplatform.dto.external.FlightSearchRequest;
import com.travelplanningplatform.dto.external.FlightSearchResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AmadeusFlightClient {

    @Value("${amadeus.api.key}")
    private String clientId;

    @Value("${amadeus.api.secret}")
    private String clientSecret;

    @Value("${amadeus.api.timeout:30}")
    private Integer timeoutSeconds;

    private Amadeus amadeus;

    private void initializeAmadeusIfNeeded() {
        if (amadeus == null) {
            this.amadeus = Amadeus.builder(clientId, clientSecret).build();
        }
    }

    @Retry(name = "amadeus")
    public Mono<FlightSearchResponse> searchFlights(FlightSearchRequest request) {
        // Validate airport codes
        String origin = request.originLocationCode();
        String destination = request.destinationLocationCode();

        if (origin == null || origin.length() != 3 || origin.matches(".*[^A-Za-z0-9].*")) {
            return Mono.error(new IllegalArgumentException("Invalid origin airport code: " + origin));
        }

        if (destination == null || destination.length() != 3 || destination.matches(".*[^A-Za-z0-9].*")) {
            return Mono.error(new IllegalArgumentException("Invalid destination airport code: " + destination));
        }


        return Mono.fromCallable(() -> {
            initializeAmadeusIfNeeded();

            try {
                // Call Amadeus API using the SDK
                FlightOfferSearch[] flightOffers = amadeus.shopping.flightOffersSearch.get(
                        Params.with("originLocationCode", origin)
                                .and("destinationLocationCode", destination)
                                .and("departureDate", request.departureDate())
                                .and("returnDate", request.returnDate())
                                .and("adults", request.adults())
                                .and("max", request.max())
                );

                if (flightOffers != null && flightOffers.length > 0) {
                    return convertToFlightSearchResponse(flightOffers);
                } else {
                    return new FlightSearchResponse();
                }
            } catch (ResponseException e) {
                return new FlightSearchResponse();
            }
        })
        .timeout(Duration.ofSeconds(timeoutSeconds));
    }

    private FlightSearchResponse convertToFlightSearchResponse(FlightOfferSearch[] flightOffers) {
       List<FlightSearchResponse.FlightOffer> offers = new ArrayList<>();

        for (FlightOfferSearch offer : flightOffers) {
                offers.add(new FlightSearchResponse.FlightOffer(
                    "flight-offer",
                    offer.getId(),
                    offer.getSource(),
                    offer.isInstantTicketingRequired(),
                    offer.isNonHomogeneous(),
                    offer.isOneWay(),
                    offer.getLastTicketingDate(),
                    offer.getNumberOfBookableSeats(),
                    null,
                    null,
                    null,
                    null,
                    null
                ));
        }

        return new FlightSearchResponse(offers, null, null);
    }
}
