package com.travelplanningplatform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripCreateRequest(
        @NotBlank
        String title,
        @NotBlank
        String destination,
        @NotNull
        LocalDate startDate,
        @NotNull
        LocalDate endDate,
        @Positive
        @NotNull
        BigDecimal totalBudget,
        @Nullable
        String currency,
        @Nullable
        String interests,
        @Min(1)
        Integer travelersCount,
        @Nullable
        String description,
        @Nullable
        Boolean isPublic
) {
}
