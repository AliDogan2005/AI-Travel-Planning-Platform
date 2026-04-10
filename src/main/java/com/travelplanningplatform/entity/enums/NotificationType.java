package com.travelplanningplatform.entity.enums;

public enum NotificationType {
    REGISTRATION("User Registration"),
    TRIP_CREATED("Trip Created"),
    ITINERARY_GENERATED("Itinerary Generated"),
    TRIP_SHARED("Trip Shared with You"),
    BUDGET_ALERT("Budget Alert"),
    TRIP_REMINDER("Trip Reminder"),
    FLIGHT_CONFIRMATION("Flight Booking Confirmation"),
    HOTEL_CONFIRMATION("Hotel Booking Confirmation"),
    PAYMENT_SUCCESS("Payment Successful"),
    PAYMENT_FAILED("Payment Failed"),
    GENERAL("General Notification");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }
}

