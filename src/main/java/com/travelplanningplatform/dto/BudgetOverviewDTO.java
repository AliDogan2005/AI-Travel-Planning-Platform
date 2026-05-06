package com.travelplanningplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetOverviewDTO {
    private Long tripId;
    private String tripTitle;
    private String currency;
    private BigDecimal totalBudget;
    private BigDecimal budgetSpent;
    private BigDecimal remainingBudget;
    private BigDecimal estimatedTotalCost;
    private BigDecimal budgetPercentage;
    private BigDecimal dailyBudget;
    private long durationDays;
    private String budgetStatus;  // HEALTHY, MODERATE, WARNING, CRITICAL, EXCEEDED
}

