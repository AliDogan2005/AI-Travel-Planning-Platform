package com.travelplanningplatform.dto;

public record AuthResponse(
    String token,
    String email,
    String username,
    String message
) {
}

