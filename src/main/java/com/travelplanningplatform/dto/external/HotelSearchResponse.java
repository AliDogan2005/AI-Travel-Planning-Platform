package com.travelplanningplatform.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HotelSearchResponse(
    List<HotelOffer> data,
    SearchMeta meta
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HotelOffer(
        String id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Integer stars,
        String website,
        String phone,
        String source,
        Double pricePerNight,
        String currency
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchMeta(
        String destination,
        Double centerLatitude,
        Double centerLongitude,
        Integer radiusMeters,
        String source,
        Integer totalResults
    ) {}

    public boolean hasResults() {
        return data != null && !data.isEmpty();
    }
}

