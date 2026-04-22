package com.travelplanningplatform.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AirportDataService {

    private final WebClient webClient;

    // Simplified cache - only store airport info by IATA code
    private final Map<String, AirportInfo> airportCache = new ConcurrentHashMap<>();
    private boolean isInitialized = false;

    public AirportDataService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer
                .build();
    }

    @PostConstruct
    public void initializeAirportData() {
        try {
            tryOpenFlightsSource().block();
            this.isInitialized = true;

        } catch (Exception e) {
            this.isInitialized = true;
        }
    }

    public record AirportInfo(
        String iataCode,
        String icaoCode,
        String name,
        String city,
        String country,
        double latitude,
        double longitude,
        String timezone
    ) {}
    public Optional<AirportInfo> getAirportInfo(String iataCode) {
        return Optional.ofNullable(airportCache.get(iataCode.toUpperCase()));
    }

    public List<AirportInfo> searchAirportsByName(String partialName) {
        String search = partialName.trim().toLowerCase();

        return airportCache.values().stream()
                .filter(airport -> airport.name() != null &&
                        (airport.name().toLowerCase().contains(search) ||
                         airport.city().toLowerCase().contains(search) ||
                         airport.iataCode().toLowerCase().contains(search)))
                .limit(20)
                .toList();
    }

    /**
     * Score airport importance based on keywords in its name
     * Higher score = more important/major airport
     */

    public List<AirportInfo> getAirportsByCountry(String country) {
        return airportCache.values().stream()
                .filter(airport -> airport.country() != null &&
                        airport.country().toLowerCase().contains(country.toLowerCase()))
                .toList();
    }

    private Mono<Boolean> tryOpenFlightsSource() {
        String url = "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat";

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseOpenFlightsData)
                .onErrorResume(error -> Mono.just(false));
    }


    private Boolean parseOpenFlightsData(String data) {
        try {
            String[] lines = data.split("\n");
            int loaded = 0;


            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                    // Use regex to handle quoted fields properly
                String[] fields = line.split(",", -1);

                if (fields.length >= 8) {
                    String name = cleanField(fields[1]);
                    String city = cleanField(fields[2]);
                    String country = cleanField(fields[3]);
                    String iata = cleanField(fields[4]);
                    String icao = cleanField(fields[5]);
                    String latStr = cleanField(fields[6]);
                    String lngStr = cleanField(fields[7]);

                    // Only include valid IATA codes (3 letters, not null)
                    if (iata != null && iata.length() == 3) {
                            double lat = Double.parseDouble(latStr != null ? latStr : "0");
                            double lng = Double.parseDouble(lngStr != null ? lngStr : "0");

                            AirportInfo airport = new AirportInfo(
                                    iata,
                                    icao != null && !icao.equals("\\N") ? icao : "",
                                    name != null ? name : "Unknown Airport",
                                    city != null ? city : "Unknown City",
                                    country != null ? country : "Unknown Country",
                                    lat, lng, ""
                            );

                            airportCache.put(iata, airport);
                            loaded++;
                    }
                }
            }

            return loaded > 0;

        } catch (Exception e) {
            return false;
        }
    }

    private String cleanField(String field) {
        if (field == null || field.equals("\\N") || field.trim().isEmpty() || field.equals("null")) {
            return null;
        }
        // Remove surrounding quotes and trim all whitespace
        String cleaned = field.replaceAll("^\"|\"$", "").trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }
    // Get statistics
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAirports", airportCache.size());
        stats.put("isInitialized", isInitialized);
        stats.put("dataSource", "OpenFlights Database");
        return stats;
    }

    // Get all unique countries in cache
    public List<String> getAllCountries() {
        return airportCache.values().stream()
                .map(AirportInfo::country)
                .distinct()
                .filter(c -> c != null && !c.isEmpty())
                .sorted()
                .toList();
    }

    public boolean isDataReady() {
        return isInitialized;
    }
}
