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
        // Load airport data asynchronously at startup
        loadAirportData()
            .doOnSuccess(success -> {
                this.isInitialized = true;
                System.out.println("✅ AirportDataService: Initialization completed successfully");
            })
            .doOnError(error -> {
                this.isInitialized = true;
                System.err.println("⚠️ AirportDataService: Initialization completed with errors: " + error.getMessage());
            })
            .subscribe();
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

    // Simple method to get airport info by IATA code
    public Optional<AirportInfo> getAirportInfo(String iataCode) {
        return Optional.ofNullable(airportCache.get(iataCode.toUpperCase()));
    }

    // Search airports by partial name
    public List<AirportInfo> searchAirportsByName(String partialName) {
        String search = partialName.toLowerCase();
        return airportCache.values().stream()
            .filter(airport -> airport.name() != null &&
                             (airport.name().toLowerCase().contains(search) ||
                              airport.city().toLowerCase().contains(search)))
            .limit(20) // Limit results
            .toList();
    }

    // Get all airports in a specific country
    public List<AirportInfo> getAirportsByCountry(String country) {
        return airportCache.values().stream()
            .filter(airport -> airport.country() != null &&
                             airport.country().toLowerCase().contains(country.toLowerCase()))
            .toList();
    }

    private Mono<Boolean> loadAirportData() {
        // Primary source: OurAirports data (more comprehensive and accurate than OpenFlights)
        // This source includes better city name mappings and is more frequently updated
        String airportDataUrl = "https://davidmegginson.github.io/ourairports-data/airports.csv";

        /* Alternative data sources you can try if OurAirports doesn't work:
         *
         * 1. IATA Airport Codes (GitHub):
         *    "https://raw.githubusercontent.com/datasets/airport-codes/master/data/airport-codes.csv"
         *
         * 2. Another OpenFlights mirror:
         *    "https://raw.githubusercontent.com/hroptatyr/openflights/master/data/airports.dat"
         *
         * 3. World Airport Codes:
         *    "https://pkgstore.datahub.io/core/airport-codes/airport-codes_csv/data/9ca22195b4c64005ac83bf75f2f5c6b4/airport-codes.csv"
         *
         * To switch to any of these, simply replace the airportDataUrl above and adjust the parsing method accordingly
         */

        return webClient.get()
            .uri(airportDataUrl)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseOurAirportsData)
            .onErrorResume(error -> {
                System.err.println("Failed to load OurAirports data, falling back to OpenFlights: " + error.getMessage());
                // Fallback to OpenFlights if OurAirports fails
                return loadOpenFlightsDataFallback();
            });
    }

    private Mono<Boolean> loadOpenFlightsDataFallback() {
        String openFlightsUrl = "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat";

        return webClient.get()
            .uri(openFlightsUrl)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseOpenFlightsData)
            .doOnError(error -> System.err.println("Failed to load OpenFlights data: " + error.getMessage()));
    }

    private Boolean parseOurAirportsData(String csvData) {
        try {
            String[] lines = csvData.split("\n");
            int loaded = 0;
            int totalLines = lines.length;

            System.out.println("🔍 AirportDataService: Processing " + totalLines + " lines from OurAirports");

            if (totalLines > 0) {
                System.out.println("🔍 AirportDataService: Header: " + lines[0]);

                // Skip header line and process data
                for (int i = 1; i < totalLines && i < 10; i++) { // Process first few lines for debugging
                    String line = lines[i].trim();
                    if (!line.isEmpty()) {
                        System.out.println("🔍 AirportDataService: Sample line " + i + ": " + line.substring(0, Math.min(100, line.length())) + "...");
                        break; // Just show one sample line
                    }
                }
            }

            // Process all lines
            for (int i = 1; i < totalLines; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                try {
                    String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    if (fields.length >= 14) {
                        String type = cleanField(fields[2]);
                        String name = cleanField(fields[3]);
                        String municipality = cleanField(fields[10]);
                        String country = cleanField(fields[8]);
                        String iata = cleanField(fields[13]);
                        String icao = cleanField(fields[12]);

                        // More lenient filtering - include all airport types with IATA codes
                        if (iata != null && iata.length() == 3 &&
                            type != null && (type.contains("airport") || type.equals("heliport"))) {

                            double lat = parseDouble(cleanField(fields[4]));
                            double lng = parseDouble(cleanField(fields[5]));

                            AirportInfo airport = new AirportInfo(
                                iata,
                                icao != null ? icao : "",
                                name != null ? name : "Unknown Airport",
                                municipality != null ? municipality : "Unknown City",
                                country != null ? country : "Unknown Country",
                                lat, lng, ""
                            );

                            airportCache.put(iata, airport);
                            loaded++;

                            // Debug: Show first successful parse
                            if (loaded == 1) {
                                System.out.println("🔍 AirportDataService: First successful parse - " +
                                                 iata + ": " + name + " in " + municipality);
                            }
                        }
                    }
                } catch (Exception lineError) {
                    // Only log first few line errors
                    if (i <= 5) {
                        System.err.println("⚠️ AirportDataService: Error parsing line " + i + ": " + lineError.getMessage());
                    }
                }
            }

            if (loaded > 0) {
                System.out.println("✅ AirportDataService: Successfully loaded " + loaded + " airports from OurAirports data");
                return true;
            } else {
                System.err.println("❌ AirportDataService: No airports loaded from OurAirports data");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ AirportDataService: Critical error parsing OurAirports data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Boolean parseOpenFlightsData(String csvData) {
        try {
            String[] lines = csvData.split("\n");
            int loaded = 0;

            for (String line : lines) {
                String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                if (fields.length >= 8) {
                    String name = cleanField(fields[1]);
                    String city = cleanField(fields[2]);
                    String country = cleanField(fields[3]);
                    String iata = cleanField(fields[4]);
                    String icao = cleanField(fields[5]);

                    if (iata != null && iata.length() == 3) {
                        double lat = parseDouble(cleanField(fields[6]));
                        double lng = parseDouble(cleanField(fields[7]));

                        AirportInfo airport = new AirportInfo(
                            iata, icao, name, city, country, lat, lng, ""
                        );

                        airportCache.put(iata, airport);
                        loaded++;
                    }
                }
            }

            System.out.println("✅ AirportDataService: Loaded " + loaded + " airports from OpenFlights data");
            return true;

        } catch (Exception e) {
            System.err.println("Error parsing OpenFlights data: " + e.getMessage());
            return false;
        }
    }

    private String cleanField(String field) {
        if (field == null || field.equals("\\N") || field.trim().isEmpty() || field.equals("null")) {
            return null;
        }
        // Remove surrounding quotes and trim whitespace
        String cleaned = field.replaceAll("^\"|\"$", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Get statistics
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAirports", airportCache.size());
        stats.put("isInitialized", isInitialized);
        stats.put("dataSource", "OurAirports (with OpenFlights fallback)");
        return stats;
    }


    public boolean isDataReady() {
        return isInitialized;
    }
}
