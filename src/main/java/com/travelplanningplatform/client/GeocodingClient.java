package com.travelplanningplatform.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class GeocodingClient {

    private final WebClient webClient;

    public GeocodingClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<GeoLocation> geocode(String query) {
        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("Query is required"));
        }

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .path("/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .build())
            .header("User-Agent", "AI-Travel-Planning-Platform/1.0")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .flatMap(response -> {
                if (response.isArray() && !response.isEmpty()) {
                    JsonNode location = response.get(0);
                    double latitude = location.path("lat").asDouble();
                    double longitude = location.path("lon").asDouble();
                    String displayName = location.path("display_name").asText(query);
                    return Mono.just(new GeoLocation(displayName, latitude, longitude));
                }
                return Mono.error(new IllegalArgumentException("Could not geocode query: " + query));
            });
    }

    public record GeoLocation(String displayName, double latitude, double longitude) {}
}

