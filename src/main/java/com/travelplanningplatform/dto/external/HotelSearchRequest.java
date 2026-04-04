package com.travelplanningplatform.dto.external;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record HotelSearchRequest(
    @NotBlank(message = "Destination is required")
    String destination,

    @Positive(message = "Radius must be a positive number")
    Integer radiusMeters,

    @Positive(message = "Max results must be a positive number")
    Integer maxResults
) {
    public static final int DEFAULT_RADIUS_METERS = 5000;
    public static final int DEFAULT_MAX_RESULTS = 20;

    public HotelSearchRequest {
        if (radiusMeters == null) {
            radiusMeters = DEFAULT_RADIUS_METERS;
        }
        if (maxResults == null) {
            maxResults = DEFAULT_MAX_RESULTS;
        }
    }
}

