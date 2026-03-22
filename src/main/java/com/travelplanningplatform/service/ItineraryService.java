package com.travelplanningplatform.service;

import com.travelplanningplatform.dto.ItineraryCreateRequest;
import com.travelplanningplatform.entity.Itinerary;
import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.exception.BadRequestException;
import com.travelplanningplatform.exception.ResourceNotFoundException;
import com.travelplanningplatform.repository.ItineraryRepository;
import com.travelplanningplatform.repository.TripRepository;
import com.travelplanningplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;

    public ItineraryService(ItineraryRepository itineraryRepository, UserRepository userRepository, TripRepository tripRepository, TripService tripService) {
        this.itineraryRepository = itineraryRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripService = tripService;
    }

    public Itinerary createItinerary(ItineraryCreateRequest dto, Long tripId, Long userId) {
        requireUserExists(userId);

        Trip trip = tripService.getTripByIdAndUser(tripId, userId);
        if (dto.title() == null || dto.title().isBlank()) {
            throw new BadRequestException("Title is required");
        }
        if (dto.content() == null || dto.content().isBlank()) {
            throw new BadRequestException("Content is required");
        }
        if (dto.location() == null || dto.location().isBlank()) {
            throw new BadRequestException("Location is required");
        }

        Itinerary itinerary = Itinerary.builder()
                .trip(trip)
                .dayNumber(dto.dayNumber())
                .date(dto.date())
                .title(dto.title())
                .content(dto.content())
                .location(dto.location())
                .estimatedCost(dto.estimatedCost())
                .userNotes(dto.userNotes())
                .generatedByAI(Boolean.TRUE.equals(dto.generatedByAI()))
                .isLocked(false)
                .build();

        return itineraryRepository.save(itinerary);
    }

    public List<Itinerary> getItinerariesByTrip(Long tripId, Long userId) {
        requireUserExists(userId);
        requireTripBelongsToUser(tripId, userId);

        return itineraryRepository.findByTripId(tripId);
    }

    public Itinerary getItineraryByTripAndDay(Long tripId, Integer dayNumber, Long userId) {
        requireUserExists(userId);
        requireTripBelongsToUser(tripId, userId);

        return itineraryRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found for trip " + tripId + " day " + dayNumber));
    }

    public List<Itinerary> getItinerariesByDateRange(Long tripId, LocalDate startDate, LocalDate endDate, Long userId) {
        requireUserExists(userId);
        requireTripBelongsToUser(tripId, userId);

        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be after or equal to start date");
        }

        return itineraryRepository.findByTripIdAndDateBetween(tripId, startDate, endDate);
    }

    public List<Itinerary> getAiGeneratedItineraries(Long tripId, Long userId) {
        requireUserExists(userId);
        requireTripBelongsToUser(tripId, userId);

        return itineraryRepository.findByTripIdAndGeneratedByAI(tripId, true);
    }

    public void deleteItinerary(Long itineraryId, Long userId) {
        requireUserExists(userId);

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + itineraryId));

        requireTripBelongsToUser(itinerary.getTrip().getId(), userId);

        if (itinerary.isLocked()) {
            throw new BadRequestException("Cannot delete locked itinerary");
        }

        itineraryRepository.delete(itinerary);
    }

    private void requireUserExists(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private void requireTripExists(Long tripId) {
        if (tripId == null || !tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip not found with id: " + tripId);
        }
    }

    private void requireTripBelongsToUser(Long tripId, Long userId) {
        requireTripExists(tripId);
        tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new BadRequestException("Trip " + tripId + " does not belong to user " + userId));
    }
}
