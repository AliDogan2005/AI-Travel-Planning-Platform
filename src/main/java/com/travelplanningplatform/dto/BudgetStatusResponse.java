package com.travelplanningplatform.dto;

import java.math.BigDecimal;

public record BudgetStatusResponse(
        String status,
        BigDecimal percentage,
        BigDecimal budgetSpent,
        BigDecimal remainingBudget
) {}