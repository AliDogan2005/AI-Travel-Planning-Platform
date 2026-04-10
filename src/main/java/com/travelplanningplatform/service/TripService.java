package com.travelplanningplatform.service;

import com.travelplanningplatform.dto.TripCreateRequest;
import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.entity.enums.TripStatus;
import com.travelplanningplatform.exception.ResourceNotFoundException;
import com.travelplanningplatform.repository.TripRepository;
import com.travelplanningplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public TripService(TripRepository tripRepository, UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    public Trip createTrip(TripCreateRequest dto, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required");
        }
        requireUserExists(user.getId());

        LocalDate startDate = dto.startDate();
        LocalDate endDate = dto.endDate();
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }

        String currency = dto.currency();
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }

        Integer travelersCount = dto.travelersCount();
        if (travelersCount == null || travelersCount < 1) {
            travelersCount = 1;
        }

        Boolean isPublic = dto.isPublic();
        if (isPublic == null) {
            isPublic = false;
        }

        Trip trip = Trip.builder()
                .user(user)
                .title(dto.title())
                .destination(dto.destination())
                .startDate(startDate)
                .endDate(endDate)
                .totalBudget(dto.totalBudget())
                .currency(currency)
                .interests(dto.interests())
                .travelersCount(travelersCount)
                .description(dto.description())
                .isPublic(isPublic)
                .status(TripStatus.PLANNED)
                .budgetSpent(BigDecimal.ZERO)
                .build();

        Trip savedTrip = tripRepository.save(trip);

        // Send trip created email
        if (emailService != null) {
            emailService.sendTripCreatedEmail(user, dto.title(), dto.destination());
        }

        return savedTrip;
    }

    public List<Trip> getTripsByUser(Long userId) {
        requireUserExists(userId);
        return tripRepository.findByUserId(userId);
    }

    public Trip getTripByIdAndUser(Long tripId, Long userId) {
        requireUserExists(userId);
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
    }

    public List<Trip> getTripsByStatus(Long userId, TripStatus status) {
        requireUserExists(userId);
        return tripRepository.findByUserIdAndStatus(userId, status);
    }

    public List<Trip> getPublicTrips() {
        return tripRepository.findByIsPublicTrue();
    }

    public void deleteTrip(Long tripId, Long userId) {
        Trip trip = getTripByIdAndUser(tripId, userId);
        tripRepository.delete(trip);
    }

    private void requireUserExists(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }
}
