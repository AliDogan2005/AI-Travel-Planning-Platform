package com.travelplanningplatform.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelplanningplatform.dto.external.HotelSearchRequest;
import com.travelplanningplatform.dto.external.HotelSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class AmadeusHotelClient {

    private static final String SOURCE_NAME = "Amadeus Hotels";
    private static final String AMADEUS_BASE_URL = "https://test.api.amadeus.com";

    private final WebClient webClient;
    private final GeocodingClient geocodingClient;

    @Value("${amadeus.api.key}")
    private String clientId;

    @Value("${amadeus.api.secret}")
    private String clientSecret;

    private String accessToken;
    private long tokenExpiry;

    public AmadeusHotelClient(WebClient.Builder webClientBuilder, GeocodingClient geocodingClient) {
        this.webClient = webClientBuilder.baseUrl(AMADEUS_BASE_URL).build();
        this.geocodingClient = geocodingClient;
    }

    public Mono<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        return geocodingClient.geocode(request.destination())
            .flatMap(geoLocation -> getAccessToken()
                .flatMap(token -> searchHotelsByCoordinates(token, request, geoLocation)))
            .onErrorResume(ex ->
                getAccessToken()
                    .flatMap(token ->
                        searchHotelsByCity(token, request)
                    )
                    .onErrorResume(ex2 ->
                       Mono.just(new HotelSearchResponse(new ArrayList<>(),
                            new HotelSearchResponse.SearchMeta(
                                request.destination(),
                                0.0, 0.0,
                                request.radiusMeters(),
                                "Amadeus",
                                0
                            )
                               )
                       )
                    )
            );
    }

    private Mono<HotelSearchResponse> searchHotelsByCoordinates(String token, HotelSearchRequest request, GeocodingClient.GeoLocation geoLocation) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(3);

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v3/shopping/hotel-offers")
                .queryParam("latitude", geoLocation.latitude())
                .queryParam("longitude", geoLocation.longitude())
                .queryParam("radius", Math.max(1, request.radiusMeters() == null ? 5000 : request.radiusMeters()))
                .queryParam("radiusUnit", "METERS")
                .queryParam("checkInDate", checkInDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("checkOutDate", checkOutDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("adults", 1)
                .queryParam("roomQuantity", 1)
                .queryParam("limit", Math.max(1, request.maxResults() == null ? 20 : request.maxResults()))
                .queryParam("sort", "PRICE")
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .header("User-Agent", "AI-Travel-Planning-Platform/1.0")
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                response ->
                   response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("Hotel search failed: " + " - " + body))))
            .bodyToMono(JsonNode.class)
            .map(response -> mapAmadeusResponse(response, request.destination(), geoLocation))
            .onErrorReturn(new HotelSearchResponse(new ArrayList<>(), 
                new HotelSearchResponse.SearchMeta(
                    request.destination(),
                    geoLocation.latitude(),
                    geoLocation.longitude(),
                    request.radiusMeters(),
                    SOURCE_NAME,
                    0
                )));
    }

    private Mono<HotelSearchResponse> searchHotelsByCity(String token, HotelSearchRequest request) {
        String cityCode = getCityCode(request.destination());

        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(3);

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v3/shopping/hotel-offers")
                .queryParam("cityCode", cityCode)
                .queryParam("checkInDate", checkInDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("checkOutDate", checkOutDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("adults", 1)
                .queryParam("limit", Math.max(1, request.maxResults() == null ? 20 : request.maxResults()))
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .header("User-Agent", "AI-Travel-Planning-Platform/1.0")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> mapAmadeusResponse(response, request.destination()))
            .onErrorReturn(new HotelSearchResponse(new ArrayList<>(), 
                new HotelSearchResponse.SearchMeta(
                    request.destination(),
                    0.0, 0.0,
                    request.radiusMeters(),
                    SOURCE_NAME,
                    0
                )));
    }

    private HotelSearchResponse mapAmadeusResponse(JsonNode response, String destination) {
        return mapAmadeusResponse(response, destination, new GeocodingClient.GeoLocation(destination, 0.0, 0.0));
    }

    private HotelSearchResponse mapAmadeusResponse(JsonNode response, String destination, GeocodingClient.GeoLocation geoLocation) {
        List<HotelSearchResponse.HotelOffer> hotels = new ArrayList<>();

        JsonNode data = response.path("data");
        if (!data.isArray()) {
            return new HotelSearchResponse(hotels, 
                new HotelSearchResponse.SearchMeta(destination, geoLocation.latitude(), geoLocation.longitude(), 0, SOURCE_NAME, 0));
        }


        for (JsonNode hotelOffer : data) {
                JsonNode hotel = hotelOffer.path("hotel");
                String hotelId = hotel.path("hotelId").asText();
                String hotelName = hotel.path("name").asText();

                JsonNode addressNode = hotel.path("address");
                StringBuilder addressBuilder = new StringBuilder();
                JsonNode lines = addressNode.path("lines");
                if (lines.isArray() && !lines.isEmpty()) {
                    addressBuilder.append(lines.get(0).asText(""));
                }
                String city = addressNode.path("cityName").asText("");
                String country = addressNode.path("countryCode").asText("");
                if (!city.isBlank()) {
                    if (!addressBuilder.isEmpty()) addressBuilder.append(", ");
                    addressBuilder.append(city);
                }
                if (!country.isBlank()) {
                    if (!addressBuilder.isEmpty()) addressBuilder.append(", ");
                    addressBuilder.append(country);
                }
                String hotelAddress = !addressBuilder.isEmpty() ? addressBuilder.toString() : "Address not available";

                Double latitude = hotel.path("latitude").asDouble();
                Double longitude = hotel.path("longitude").asDouble();
                int rating = hotel.path("rating").asInt();

                Double pricePerNight = null;
                String currency = "USD";

                JsonNode offers = hotelOffer.path("offers");
                if (offers.isArray() && !offers.isEmpty()) {
                    JsonNode offer = offers.get(0);

                    JsonNode priceNode = offer.path("price");
                    String base = priceNode.path("base").asText(null);
                    String total = priceNode.path("total").asText(null);
                    currency = priceNode.path("currency").asText("USD");

                    if (base != null && !base.isBlank()) {
                            pricePerNight = Double.parseDouble(base);
                    } 
                    if (pricePerNight == null && total != null && !total.isBlank()) {
                            pricePerNight = Double.parseDouble(total);
                    }
                }

                String website = hotel.path("website").asText(null);
                String phone = hotel.path("contact").path("phone").asText(null);

                hotels.add(new HotelSearchResponse.HotelOffer(
                    hotelId,
                    hotelName,
                    hotelAddress,
                    latitude,
                    longitude,
                    0.0,
                    rating > 0 ? rating : null,
                    website,
                    phone,
                    SOURCE_NAME,
                    pricePerNight,
                    currency
                ));
        }

        return new HotelSearchResponse(hotels, 
            new HotelSearchResponse.SearchMeta(
                destination,
                geoLocation.latitude(),
                geoLocation.longitude(),
                0,
                SOURCE_NAME,
                hotels.size()
            ));
    }


    private Mono<String> getAccessToken() {
        // Return cached token if still valid
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
            .onStatus(status -> !status.is2xxSuccessful(),
                response ->
                    response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("Amadeus auth failed: " + " - " + body)))
                )
            .bodyToMono(JsonNode.class)
            .map(response -> {
                String token = response.path("access_token").asText();
                int expiresIn = response.path("expires_in").asInt(3600);
                tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L);
                return token;
            });
    }

    private String getCityCode(String destination) {
        String lower = destination.toLowerCase();
        if (lower.contains("athens")) return "ATH";
        if (lower.contains("paris")) return "PAR";
        if (lower.contains("london")) return "LON";
        if (lower.contains("rome")) return "ROM";
        if (lower.contains("barcelona")) return "BCN";
        if (lower.contains("madrid")) return "MAD";
        if (lower.contains("berlin")) return "BER";
        if (lower.contains("amsterdam")) return "AMS";
        if (lower.contains("venice")) return "VCE";
        if (lower.contains("istanbul")) return "IST";
        // Default: try first 3 letters
        return destination.substring(0, Math.min(3, destination.length())).toUpperCase();
    }
}

