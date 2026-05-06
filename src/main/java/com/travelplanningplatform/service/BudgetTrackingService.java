package com.travelplanningplatform.service;

import com.travelplanningplatform.dto.BudgetOverviewDTO;
import com.travelplanningplatform.entity.Itinerary;
import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.exception.ResourceNotFoundException;
import com.travelplanningplatform.repository.ItineraryRepository;
import com.travelplanningplatform.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;



@Service
@RequiredArgsConstructor
public class BudgetTrackingService {

    private final TripRepository tripRepository;
    private final ItineraryRepository itineraryRepository;
    private static final String TRIP_NOT_FOUND = "Trip not found";

    public BudgetOverviewDTO getBudgetOverview(Long tripId, Long userId) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));

        List<Itinerary> itineraries = itineraryRepository.findByTripId(tripId);
        BigDecimal estimatedTotalCost = calculateEstimatedTotalCost(itineraries);

        return BudgetOverviewDTO.builder()
                .tripId(tripId)
                .tripTitle(trip.getTitle())
                .currency(trip.getCurrency())
                .totalBudget(trip.getTotalBudget())
                .budgetSpent(trip.getBudgetSpent())
                .remainingBudget(trip.getRemainingBudget())
                .estimatedTotalCost(estimatedTotalCost)
                .budgetPercentage(calculateBudgetPercentage(trip.getTotalBudget(), trip.getBudgetSpent()))
                .dailyBudget(trip.getDailyBudget())
                .durationDays(trip.getDurationDays())
                .budgetStatus(getBudgetStatus(trip.getTotalBudget(), trip.getBudgetSpent()))
                .build();
    }

    public void updateBudgetSpent(Long tripId, Long userId, BigDecimal amount) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));

        BigDecimal newSpent = trip.getBudgetSpent().add(amount);

        if (newSpent.compareTo(trip.getTotalBudget()) > 0) {
            throw new IllegalArgumentException("Budget exceeded: " + newSpent + " > " + trip.getTotalBudget());
        }

        trip.setBudgetSpent(newSpent);
        tripRepository.save(trip);
    }

    public String getBudgetStatus(BigDecimal totalBudget, BigDecimal spent) {
        BigDecimal percentage = calculateBudgetPercentage(totalBudget, spent);

        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "EXCEEDED";
        } else if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return "CRITICAL";
        } else if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "WARNING";
        } else if (percentage.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "MODERATE";
        }
        return "HEALTHY";
    }


    public BigDecimal calculateBudgetPercentage(BigDecimal totalBudget, BigDecimal spent) {
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spent.divide(totalBudget, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateEstimatedTotalCost(List<Itinerary> itineraries) {
        return itineraries.stream()
                .map(Itinerary::getEstimatedCost)
                .filter(cost -> cost != null && cost.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingDailyBudget(Long tripId, Long userId) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));
        return trip.getDailyBudget().subtract(trip.getBudgetSpent().divide(BigDecimal.valueOf(trip.getDurationDays()), 2, RoundingMode.HALF_UP));
    }

    public boolean isBudgetAvailable(Long tripId, Long userId, BigDecimal requiredAmount) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND));
        return trip.getRemainingBudget().compareTo(requiredAmount) >= 0;
    }

    /**
     * Get budget summary for all user trips
     */
    public List<BudgetOverviewDTO> getAllTripsBudgetSummary(Long userId) {
        List<Trip> trips = tripRepository.findByUserId(userId);
        return trips.stream()
                .map(trip -> getBudgetOverview(trip.getId(), userId))
                .toList();
    }
}


