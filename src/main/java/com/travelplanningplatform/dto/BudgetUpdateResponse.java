package com.travelplanningplatform.dto;

public record BudgetUpdateResponse(
        String message,
        BudgetOverviewDTO budget
) {}
