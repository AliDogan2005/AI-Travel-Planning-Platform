package com.travelplanningplatform.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.travelplanningplatform.dto.ItineraryCreateRequest;
import com.travelplanningplatform.dto.ItineraryGenerateRequest;
import com.travelplanningplatform.entity.Itinerary;
import com.travelplanningplatform.entity.Trip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ItineraryGenerationService {

    private final ItineraryService itineraryService;
    private final TripService tripService;
    private final Client geminiClient;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String modelName;

    public ItineraryGenerationService(ItineraryService itineraryService,
                                    TripService tripService,
                                    @Autowired(required = false) Client geminiClient) {
        this.itineraryService = itineraryService;
        this.tripService = tripService;
        this.geminiClient = geminiClient;
    }

    @Transactional
    public List<Itinerary> generateItinerary(ItineraryGenerateRequest request, Long userId) {
        Trip trip = tripService.getTripByIdAndUser(request.tripId(), userId);

        long dayCount = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;

        if (geminiClient == null) {
            throw new RuntimeException("Google Gemini API service is not available. Please check your Gemini API key configuration.");
        }

        String prompt = buildGeminiPrompt(trip, request, dayCount);

        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                modelName,
                prompt,
                null
            );

            String aiResponse = response.text();

            if (aiResponse == null || aiResponse.isEmpty()) {
                throw new RuntimeException("Gemini returned empty response");
            }

            return parseAndSaveItineraries(aiResponse, trip, userId, dayCount);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate itinerary using Google Gemini: " + e.getMessage(), e);
        }
    }

    private String buildGeminiPrompt(Trip trip, ItineraryGenerateRequest request, long dayCount) {
        return String.format("""
            Create a detailed %d-day travel itinerary for %s.
            
            Trip Details:
            - Destination: %s
            - Start Date: %s
            - End Date: %s
            - Budget: %s %s (total for %d travelers)
            - Number of Travelers: %d
            - Interests: %s
            - Travel Style: %s
            - Additional Requirements: %s
            
            Please create a day-by-day itinerary with the following EXACT format for each day:
            
            DAY [number]:
            TITLE: [Engaging title for the day]
            LOCATION: [Main area/district for activities]
            MORNING:
            - [Specific morning activity with time and estimated cost]
            AFTERNOON:
            - [Specific afternoon activity with time and estimated cost]
            EVENING:
            - [Specific evening activity with time and estimated cost]
            COST: [Total estimated daily cost in %s]
            
            Requirements:
            1. Stay within total budget of %s %s
            2. Include specific attractions, restaurants, and activities
            3. Consider travel time between locations
            4. Match interests: %s
            5. Adapt to travel style: %s
            6. Include realistic cost estimates
            7. Make each day engaging and well-balanced
            8. Use the EXACT format above for each day
            """,
            dayCount, trip.getDestination(),
            trip.getDestination(),
            trip.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            trip.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            request.budget(), trip.getCurrency(), trip.getTravelersCount(),
            trip.getTravelersCount(),
            request.interests(),
            request.travelStyle(),
            request.additionalRequirements() != null ? request.additionalRequirements() : "None",
            trip.getCurrency(),
            request.budget(), trip.getCurrency(),
            request.interests(),
            request.travelStyle()
        );
    }

    private List<Itinerary> parseAndSaveItineraries(String aiResponse, Trip trip, Long userId, long dayCount) {
        List<Itinerary> itineraries = new ArrayList<>();

        String[] dayBlocks = aiResponse.split("(?=DAY \\d+:)");

        for (String dayBlock : dayBlocks) {
            Itinerary itinerary = processDayBlock(dayBlock, trip, userId, dayCount, itineraries.size());
            if (itinerary != null) {
                itineraries.add(itinerary);
            }
        }

        return itineraries;
    }

    private Itinerary processDayBlock(String dayBlock, Trip trip, Long userId, long dayCount, int currentItineraryCount) {
        if (dayBlock.trim().isEmpty() || !dayBlock.contains("DAY")) {
            return null;
        }

        try {
            int dayNumber = extractDayNumber(dayBlock);
            if (dayNumber < 1 || dayNumber > dayCount) {
                return null;
            }

            LocalDate date = trip.getStartDate().plusDays((long) dayNumber - 1);
            String title = extractField(dayBlock, "TITLE:", "LOCATION:");
            String location = extractField(dayBlock, "LOCATION:", "MORNING:");
            String morning = extractField(dayBlock, "MORNING:", "AFTERNOON:");
            String afternoon = extractField(dayBlock, "AFTERNOON:", "EVENING:");
            String evening = extractField(dayBlock, "EVENING:", "COST:");
            String costStr = extractField(dayBlock, "COST:", null);

            String content = formatItineraryContent(morning, afternoon, evening);
            BigDecimal estimatedCost = parseCost(costStr, trip.getCurrency());

            ItineraryCreateRequest createRequest = new ItineraryCreateRequest(
                dayNumber,
                date,
                title.trim(),
                content,
                location.trim(),
                estimatedCost,
                null,
                true
            );

            return itineraryService.createItinerary(createRequest, trip.getId(), userId);

        } catch (Exception e) {
            int dayNumber = currentItineraryCount + 1;
            if (dayNumber <= dayCount) {
                ItineraryCreateRequest fallbackRequest = getItineraryCreateRequest(trip, dayBlock, dayNumber);
                return itineraryService.createItinerary(fallbackRequest, trip.getId(), userId);
            }
            return null;
        }
    }

    private static ItineraryCreateRequest getItineraryCreateRequest(Trip trip, String dayBlock, int dayNumber) {
        LocalDate date = trip.getStartDate().plusDays((long) dayNumber - 1);
        return new ItineraryCreateRequest(
                dayNumber,
            date,
            "Day " + dayNumber + " in " + trip.getDestination(),
            "AI-generated content parsing failed. Please edit manually.\n\nOriginal AI Response:\n" + dayBlock,
            trip.getDestination(),
            BigDecimal.ZERO,
            "AI Generated - Parse Failed",
            true
        );
    }

    private int extractDayNumber(String dayBlock) {
        Pattern dayPattern = Pattern.compile("DAY (\\d+):");
        Matcher matcher = dayPattern.matcher(dayBlock);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new RuntimeException("Could not extract day number");
    }

    private String extractField(String content, String startMarker, String endMarker) {
        int startIndex = content.indexOf(startMarker);
        if (startIndex == -1) return "";

        startIndex += startMarker.length();

        int endIndex;
        if (endMarker != null) {
            endIndex = content.indexOf(endMarker, startIndex);
            if (endIndex == -1) endIndex = content.length();
        } else {
            endIndex = content.length();
        }

        return content.substring(startIndex, endIndex).trim();
    }

    private String formatItineraryContent(String morning, String afternoon, String evening) {
        StringBuilder content = new StringBuilder();

        if (!morning.isEmpty()) {
            content.append("MORNING:\n").append(morning.trim()).append("\n\n");
        }

        if (!afternoon.isEmpty()) {
            content.append("AFTERNOON:\n").append(afternoon.trim()).append("\n\n");
        }

        if (!evening.isEmpty()) {
            content.append("EVENING:\n").append(evening.trim()).append("\n");
        }

        return content.toString().trim();
    }

    private BigDecimal parseCost(String costStr, String currency) {
        if (costStr == null || costStr.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            String cleanedCost = costStr.trim()
                .replaceAll("[€$£¥₹]", "") // Remove common currency symbols
                .replaceAll(currency, "") // Remove the specific currency code
                .replaceAll("[^0-9.,]", "") // Remove everything except numbers, dots, and commas
                .replace(",", "."); // Normalize decimal separator

            Pattern numberPattern = Pattern.compile("\\d+(?:\\.\\d+)?");
            Matcher matcher = numberPattern.matcher(cleanedCost);
            if (matcher.find()) {
                BigDecimal cost = new BigDecimal(matcher.group());
                System.out.println("Parsed cost: " + cost + " " + currency + " from: " + costStr);
                return cost;
            }
        } catch (Exception e) {
            System.err.println("Failed to parse cost '" + costStr + "' for currency " + currency + ": " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }
}
