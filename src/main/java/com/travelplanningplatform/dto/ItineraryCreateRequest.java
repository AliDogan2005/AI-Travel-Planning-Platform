package com.travelplanningplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItineraryCreateRequest(
    @NotNull
    Integer dayNumber,
    @NotNull
    LocalDate date,
    @NotBlank
    String title,
    @NotBlank
    String content,
    @NotBlank
    String location,
    @Positive
    BigDecimal estimatedCost,

    String userNotes,

    Boolean generatedByAI
) {
}
