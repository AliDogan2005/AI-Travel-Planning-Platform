package com.travelplanningplatform.client;

import com.travelplanningplatform.dto.external.FlightSearchRequest;
import com.travelplanningplatform.dto.external.FlightSearchResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class AmadeusFlightClient {

    private final WebClient webClient;

    @Value("${amadeus.api.key}")
    private String clientId;

    @Value("${amadeus.api.secret}")
    private String clientSecret;

    @Value("${amadeus.api.timeout:30}")
    private Integer timeoutSeconds;

    private String accessToken;
    private long tokenExpirationTime = 0;

    public AmadeusFlightClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://test.api.amadeus.com")
            .build();
    }
    @Retry(name = "amadeus")
    public Mono<FlightSearchResponse> searchFlights(FlightSearchRequest request) {
        System.out.println("🔑 AmadeusClient: Starting flight search - " + request.originLocationCode() + " → " + request.destinationLocationCode());

        return getAccessToken()
            .doOnSuccess(token -> System.out.println("✅ AmadeusClient: Got access token"))
            .doOnError(error -> System.err.println("❌ AmadeusClient: Failed to get access token: " + error.getMessage()))
            .flatMap(token -> {
                System.out.println("🌐 AmadeusClient: Making API call to Amadeus...");
                return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path("/v2/shopping/flight-offers")
                        .queryParam("originLocationCode", request.originLocationCode())
                        .queryParam("destinationLocationCode", request.destinationLocationCode())
                        .queryParam("departureDate", request.departureDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .queryParam("adults", request.adults());

                    // Add optional parameters
                    if (request.returnDate() != null) {
                        builder.queryParam("returnDate", request.returnDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    }
                    if (request.children() != null && request.children() > 0) {
                        builder.queryParam("children", request.children());
                    }
                    if (request.infants() != null && request.infants() > 0) {
                        builder.queryParam("infants", request.infants());
                    }
                    if (request.travelClass() != null) {
                        builder.queryParam("travelClass", request.travelClass());
                    }
                    if (request.nonStop() != null) {
                        builder.queryParam("nonStop", request.nonStop());
                    }
                    if (request.currencyCode() != null) {
                        builder.queryParam("currencyCode", request.currencyCode());
                    }
                    if (request.max() != null) {
                        builder.queryParam("max", request.max());
                    }

                    return builder.build();
                })
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                        System.err.println("❌ AmadeusClient: API Error - HTTP " + clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Amadeus API Error: " + clientResponse.statusCode()));
                    })
                .bodyToMono(FlightSearchResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> {
                    if (response != null && response.hasResults()) {
                        System.out.println("✅ AmadeusClient: Successfully received " + response.getOfferCount() + " flight offers");
                    } else {
                        System.out.println("⚠️ AmadeusClient: API call successful but no flights returned");
                    }
                })
                .doOnError(error -> System.err.println("❌ AmadeusClient: API call failed: " + error.getMessage()));
            });
    }

    private Mono<String> getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpirationTime - 300000) {
            return Mono.just(accessToken);
        }

        return webClient.post()
            .uri("/v1/security/oauth2/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                this.accessToken = (String) response.get("access_token");
                Integer expiresIn = (Integer) response.get("expires_in");
                this.tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000L);

                return this.accessToken;
            })
            .doOnError(error -> System.err.println("Failed to get Amadeus access token: " + error.getMessage()));
    }
}
