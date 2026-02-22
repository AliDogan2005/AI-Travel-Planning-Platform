package com.travelplanningplatform.repository;

import com.travelplanningplatform.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findByTripId(Long tripId);

    Optional<Itinerary> findByTripIdAndDayNumber(Long tripId , Integer dayNumber);

    List<Itinerary> findByTripIdAndDateBetween(Long tripId, LocalDate startDate, LocalDate endDate);

    List<Itinerary> findByTripIdAndGeneratedByAI(Long tripId, boolean generatedByAI);
}
