package com.travelplanningplatform.repository;

import com.travelplanningplatform.entity.Trip;
import com.travelplanningplatform.entity.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserId(Long userId);

    Optional<Trip> findByIdAndUserId(Long id, Long userId);

    List<Trip> findByUserIdAndStatus(Long userId, TripStatus status);

    List<Trip> findByIsPublicTrue();



}
