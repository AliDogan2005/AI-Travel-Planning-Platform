package com.travelplanningplatform.dto.external;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record FlightSearchRequest(
    @NotBlank
    String originLocationCode,

    @NotBlank
    String destinationLocationCode,

    @NotNull
    LocalDate departureDate,

    LocalDate returnDate,

    @Positive
    Integer adults,

    Integer children,

    Integer infants,

    String travelClass, // ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST

    Boolean nonStop,

    String currencyCode,

    Integer max
) {
}


