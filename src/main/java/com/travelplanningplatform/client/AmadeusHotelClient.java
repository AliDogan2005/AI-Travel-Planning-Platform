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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AmadeusHotelClient {

    private static final String SOURCE_NAME = "Amadeus Hotels";
    private static final String AMADEUS_BASE_URL = "https://test.api.amadeus.com";

    private final WebClient webClient;

    @Value("${amadeus.api.key}")
    private String clientId;

    @Value("${amadeus.api.secret}")
    private String clientSecret;

    private String accessToken;
    private long tokenExpiry;

    public AmadeusHotelClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(AMADEUS_BASE_URL).build();
    }

    public Mono<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        System.out.println("DEBUG: Starting hotel search for destination: " + request.destination());
        return geocodeDestination(request.destination())
            .doOnNext(geo -> System.out.println("DEBUG: Geocoded to lat=" + geo.latitude() + ", lon=" + geo.longitude()))
            .flatMap(geoLocation -> getAccessToken()
                .flatMap(token -> {
                    System.out.println("DEBUG: Got Amadeus token, searching by coordinates");
                    return searchHotelsByCoordinates(token, request, geoLocation);
                }))
            .onErrorResume(ex -> {
                System.err.println("DEBUG: Error in coordinate search, falling back to city code: " + ex.getMessage());
                return getAccessToken()
                    .flatMap(token -> {
                        System.out.println("DEBUG: Searching by city code fallback");
                        return searchHotelsByCity(token, request);
                    });
            });
    }

    private Mono<GeoLocation> geocodeDestination(String destination) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .path("/search")
                .queryParam("q", destination)
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
                    String displayName = location.path("display_name").asText(destination);
                    return Mono.just(new GeoLocation(displayName, latitude, longitude));
                }
                return Mono.error(new IllegalArgumentException("Could not geocode destination: " + destination));
            });
    }

    private Mono<HotelSearchResponse> searchHotelsByCoordinates(String token, HotelSearchRequest request, GeoLocation geoLocation) {
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
                response -> {
                    return response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("Hotel search failed: " + " - " + body)));
                })
            .bodyToMono(JsonNode.class)
            .doOnNext(response -> System.out.println("DEBUG: Hotel API Response: " + response.toPrettyString()))
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
        return mapAmadeusResponse(response, destination, new GeoLocation(destination, 0.0, 0.0));
    }

    private HotelSearchResponse mapAmadeusResponse(JsonNode response, String destination, GeoLocation geoLocation) {
        List<HotelSearchResponse.HotelOffer> hotels = new ArrayList<>();

        System.out.println("DEBUG: Parsing Amadeus Hotel List API response");

        JsonNode data = response.path("data");
        if (!data.isArray()) {
            System.out.println("DEBUG: No data array found in response");
            return new HotelSearchResponse(hotels, 
                new HotelSearchResponse.SearchMeta(destination, geoLocation.latitude(), geoLocation.longitude(), 0, SOURCE_NAME, 0));
        }

        System.out.println("DEBUG: Found " + data.size() + " hotels in response");

        for (JsonNode hotelOffer : data) {
            try {
                // Parse hotel information
                JsonNode hotel = hotelOffer.path("hotel");
                String hotelId = hotel.path("hotelId").asText();
                String hotelName = hotel.path("name").asText();
                
                // Build address from hotel data
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

                // Parse pricing information from offers
                Double pricePerNight = null;
                String currency = "USD";

                JsonNode offers = hotelOffer.path("offers");
                if (offers.isArray() && offers.size() > 0) {
                    JsonNode offer = offers.get(0);
                    
                    // Get price from the offer - Hotel List API returns base and total
                    JsonNode priceNode = offer.path("price");
                    String base = priceNode.path("base").asText(null);
                    String total = priceNode.path("total").asText(null);
                    currency = priceNode.path("currency").asText("USD");
                    
                    // Try to parse base or total price
                    if (base != null && !base.isBlank()) {
                        try {
                            pricePerNight = Double.parseDouble(base);
                        } catch (NumberFormatException e) {
                            System.out.println("DEBUG: Could not parse base price: " + base);
                        }
                    } 
                    if (pricePerNight == null && total != null && !total.isBlank()) {
                        try {
                            pricePerNight = Double.parseDouble(total);
                        } catch (NumberFormatException e) {
                            System.out.println("DEBUG: Could not parse total price: " + total);
                        }
                    }
                }

                String website = hotel.path("website").asText(null);
                String phone = hotel.path("contact").path("phone").asText(null);

                System.out.println("DEBUG: Parsed hotel - " + hotelName + " at $" + pricePerNight + " " + currency);

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
            } catch (Exception e) {
                System.err.println("DEBUG: Error parsing hotel: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("DEBUG: Total hotels parsed: " + hotels.size());

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

        System.out.println("DEBUG: Authenticating with Amadeus API using Basic Auth");
        System.out.println("DEBUG: Client ID: " + clientId.substring(0, Math.min(5, clientId.length())) + "***");

        return webClient.post()
            .uri("/v1/security/oauth2/token")
            .header("Authorization", "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials")
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                response -> {
                    return response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("Amadeus auth failed: " + " - " + body)));
                })
            .bodyToMono(JsonNode.class)
            .map(response -> {
                String token = response.path("access_token").asText();
                int expiresIn = response.path("expires_in").asInt(3600);
                tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L);
                System.out.println("DEBUG: Successfully obtained Amadeus token. Expires in: " + expiresIn + "s");
                return token;
            })
            .doOnError(error -> System.err.println("DEBUG: Amadeus authentication error: " + error.getMessage()));
    }

    private String getCityCode(String destination) {
        // Simplified city code mapping
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

    private record GeoLocation(String displayName, double latitude, double longitude) {}
}

