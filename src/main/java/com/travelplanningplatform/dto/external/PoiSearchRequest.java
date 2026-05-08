package com.travelplanningplatform.dto.external;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

public record PoiSearchRequest(
    String query,

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double latitude,

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double longitude,

    @Positive(message = "Radius must be a positive number")
    Integer radiusMeters,

    String categories,

    @Positive(message = "Limit must be a positive number")
    Integer limit
) {
    public static final int DEFAULT_RADIUS_METERS = 5000;
    public static final int DEFAULT_LIMIT = 20;

    public PoiSearchRequest {
        if (radiusMeters == null) {
            radiusMeters = DEFAULT_RADIUS_METERS;
        }
        if (limit == null) {
            limit = DEFAULT_LIMIT;
        }
    }
}

