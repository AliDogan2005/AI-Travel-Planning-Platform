package com.travelplanningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItineraryGenerateRequest(
    @NotNull
    Long tripId,

    @NotBlank
    String interests,

    @NotBlank
    String travelStyle,

    @Positive
    BigDecimal budget,

    String additionalRequirements
) {}
