package com.travelplanningplatform.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightSearchResponse(
    List<FlightOffer> data,
    Map<String, Object> meta,
    Object dictionaries
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlightOffer(
        String type,
        String id,
        String source,
        Boolean instantTicketingRequired,
        Boolean nonHomogeneous,
        Boolean oneWay,
        String lastTicketingDate,
        Integer numberOfBookableSeats,
        List<Itinerary> itineraries,
        Price price,
        Object pricingOptions,
        Object validatingAirlineCodes,
        Object travelerPricings
    ) {
        // Constructor with defaults for optional fields
        public FlightOffer(String type, String id, String source, List<Itinerary> itineraries, Price price) {
            this(type, id, source, null, null, null, null, null, itineraries, price, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Itinerary(
        String duration,
        List<Segment> segments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
        Departure departure,
        Arrival arrival,
        String carrierCode,
        String number,
        Aircraft aircraft,
        Operating operating,
        String duration,
        String id,
        Integer numberOfStops,
        Boolean blacklistedInEU
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Departure(
        String iataCode,
        String terminal,
        LocalDateTime at
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Arrival(
        String iataCode,
        String terminal,
        String at
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Aircraft(
        String code
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Operating(
        String carrierCode
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(
        String currency,
        String total,
        String base,
        Object fees,
        String grandTotal
    ) {
        // Constructor for simplified price creation
        public Price(String currency, String total) {
            this(currency, total, null, null, total);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fee(
        String amount,
        String type
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PricingOptions(
        List<String> fareType,
        Boolean includedCheckedBagsOnly
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TravelerPricing(
        String travelerId,
        String fareOption,
        String travelerType,
        Price price,
        List<FareDetailsBySegment> fareDetailsBySegment
    ) {
        // Constructor with defaults
        public TravelerPricing(String travelerId, String travelerType, Price price) {
            this(travelerId, null, travelerType, price, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FareDetailsBySegment(
        String segmentId,
        String cabin,
        String fareBasis,
        String brandedFare,
        String segmentClass,
        IncludedCheckedBags includedCheckedBags
    ) {
        // Constructor with minimal required fields
        public FareDetailsBySegment(String segmentId, String cabin) {
            this(segmentId, cabin, null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncludedCheckedBags(
        Integer quantity,
        Integer weight,
        String weightUnit
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dictionaries(
        Map<String, String> locations,
        Map<String, String> aircraft,
        Map<String, String> currencies,
        Map<String, String> carriers
    ) {}

    public FlightSearchResponse() {
        this(List.of(), Map.of(), null);
    }

    public boolean hasResults() {
        return data != null && !data.isEmpty();
    }

    public int getOfferCount() {
        return data != null ? data.size() : 0;
    }

    public FlightOffer getCheapestOffer() {
        if (data == null || data.isEmpty()) {
            return null;
        }

        return data.stream()
            .min((offer1, offer2) -> {
                BigDecimal price1 = new BigDecimal(offer1.price().total());
                BigDecimal price2 = new BigDecimal(offer2.price().total());
                return price1.compareTo(price2);
            })
            .orElse(null);
    }
}

