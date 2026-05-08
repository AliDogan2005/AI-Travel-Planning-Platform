package com.travelplanningplatform.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplanningplatform.dto.external.PoiSearchRequest;
import com.travelplanningplatform.dto.external.PoiSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class AmadeusPoiClient {

    private static final String SOURCE_NAME = "Amadeus POI";
    private static final String AMADEUS_BASE_URL = "https://test.api.amadeus.com";

    private final WebClient webClient;
    private final GeocodingClient geocodingClient;

    @Value("${amadeus.api.key}")
    private String clientId;

    @Value("${amadeus.api.secret}")
    private String clientSecret;

    private String accessToken;
    private long tokenExpiry;

    public AmadeusPoiClient(WebClient.Builder webClientBuilder, GeocodingClient geocodingClient) {
        this.webClient = webClientBuilder.baseUrl(AMADEUS_BASE_URL).build();
        this.geocodingClient = geocodingClient;
    }

    public Mono<PoiSearchResponse> searchPois(PoiSearchRequest request) {
        return resolveCoordinates(request)
            .flatMap(geo -> getAccessToken()
                .flatMap(token -> searchByCoordinates(token, request, geo)))
            .onErrorReturn(emptyResponse(request, null));
    }

    private Mono<GeocodingClient.GeoLocation> resolveCoordinates(PoiSearchRequest request) {
        if (request.latitude() != null && request.longitude() != null) {
            return Mono.just(new GeocodingClient.GeoLocation("custom", request.latitude(), request.longitude()));
        }
        if (request.query() == null || request.query().isBlank()) {
            return Mono.error(new IllegalArgumentException("Query or coordinates are required"));
        }
        return geocodingClient.geocode(request.query());
    }

    private Mono<PoiSearchResponse> searchByCoordinates(String token, PoiSearchRequest request, GeocodingClient.GeoLocation geo) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/reference-data/locations/pois")
                .queryParam("latitude", geo.latitude())
                .queryParam("longitude", geo.longitude())
                .queryParam("radius", Math.max(1, request.radiusMeters()))
                .queryParamIfPresent("categories", request.categories() == null || request.categories().isBlank()
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(request.categories()))
                .queryParam("page[limit]", Math.max(1, request.limit()))
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .header("User-Agent", "AI-Travel-Planning-Platform/1.0")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> mapAmadeusResponse(response, request, geo))
            .onErrorReturn(emptyResponse(request, geo));
    }

    private PoiSearchResponse mapAmadeusResponse(JsonNode response, PoiSearchRequest request, GeocodingClient.GeoLocation geo) {
        List<PoiSearchResponse.PoiItem> items = new ArrayList<>();
        JsonNode data = response.path("data");
        if (data.isArray()) {
            for (JsonNode poi : data) {
                JsonNode geoCode = poi.path("geoCode");
                Double latitude = geoCode.path("latitude").isMissingNode() ? null : geoCode.path("latitude").asDouble();
                Double longitude = geoCode.path("longitude").isMissingNode() ? null : geoCode.path("longitude").asDouble();

                Integer distanceMeters = parseDistanceMeters(poi.path("distance"));

                List<String> tags = new ArrayList<>();
                JsonNode tagNode = poi.path("tags");
                if (tagNode.isArray()) {
                    for (JsonNode tag : tagNode) {
                        tags.add(tag.asText());
                    }
                }

                items.add(new PoiSearchResponse.PoiItem(
                    poi.path("id").asText(null),
                    poi.path("name").asText(null),
                    poi.path("category").asText(null),
                    poi.path("rank").isMissingNode() ? null : poi.path("rank").asInt(),
                    tags.isEmpty() ? null : tags,
                    latitude,
                    longitude,
                    distanceMeters,
                    SOURCE_NAME
                ));
            }
        }

        PoiSearchResponse.SearchMeta meta = new PoiSearchResponse.SearchMeta(
            geo == null ? null : geo.latitude(),
            geo == null ? null : geo.longitude(),
            request.radiusMeters(),
            items.size(),
            SOURCE_NAME
        );

        return new PoiSearchResponse(items, meta);
    }

    private Integer parseDistanceMeters(JsonNode distanceNode) {
        if (distanceNode == null || distanceNode.isMissingNode() || distanceNode.isNull()) {
            return null;
        }
        if (distanceNode.isNumber()) {
            return distanceNode.asInt();
        }
        if (distanceNode.isObject()) {
            double value = distanceNode.path("value").asDouble(0.0);
            String unit = distanceNode.path("unit").asText("KM");
            if ("KM".equalsIgnoreCase(unit)) {
                return (int) Math.round(value * 1000.0);
            }
            if ("M".equalsIgnoreCase(unit) || "METERS".equalsIgnoreCase(unit)) {
                return (int) Math.round(value);
            }
        }
        return null;
    }

    private Mono<String> getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return Mono.just(accessToken);
        }

        String credentials = java.util.Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes());

        return webClient.post()
            .uri("/v1/security/oauth2/token")
            .header("Authorization", "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
                String token = response.path("access_token").asText();
                int expiresIn = response.path("expires_in").asInt(3600);
                tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L);
                return token;
            });
    }

    private PoiSearchResponse emptyResponse(PoiSearchRequest request, GeocodingClient.GeoLocation geo) {
        PoiSearchResponse.SearchMeta meta = new PoiSearchResponse.SearchMeta(
            geo == null ? null : geo.latitude(),
            geo == null ? null : geo.longitude(),
            request.radiusMeters(),
            0,
            SOURCE_NAME
        );
        return new PoiSearchResponse(new ArrayList<>(), meta);
    }
}
