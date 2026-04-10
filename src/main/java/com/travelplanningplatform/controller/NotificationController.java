package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.NotificationResponse;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.entity.enums.NotificationType;
import com.travelplanningplatform.repository.UserRepository;
import com.travelplanningplatform.service.EmailService;
import com.travelplanningplatform.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all notifications for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        List<NotificationResponse> notifications = emailService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        long count = emailService.getUnreadNotificationsCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get notifications by type
     */
    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByType(
            @PathVariable String type,
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
        List<NotificationResponse> notifications = emailService.getNotificationsByType(userId, notificationType);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<String> retryFailedNotifications() {
        emailService.retrySendingFailedNotifications();
        return ResponseEntity.ok("Retry process started for failed notifications");
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Email notification service is running");
    }

    private Long extractUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            String username = jwtUtil.extractUsername(token);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found for username: " + username));
            return user.getId();
        } catch (Exception e) {
            log.error("Failed to extract user ID from token", e);
            throw new RuntimeException("Invalid token or user not found");
        }
    }
}

