package com.travelplanningplatform.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PoiSearchResponse(
    List<PoiItem> data,
    SearchMeta meta
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PoiItem(
        String id,
        String name,
        String category,
        Integer rank,
        List<String> tags,
        Double latitude,
        Double longitude,
        Integer distanceMeters,
        String source
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchMeta(
        Double centerLatitude,
        Double centerLongitude,
        Integer radiusMeters,
        Integer totalResults,
        String source
    ) {}

    public boolean hasResults() {
        return data != null && !data.isEmpty();
    }

    public List<PoiItem> getResults() {
        return data != null ? data : List.of();
    }
}

