package com.travelplanningplatform.dto;

import com.travelplanningplatform.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private String recipientEmail;
    private String subject;
    private String body;
    private NotificationType type;
    private boolean sent;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String sendAttemptError;
}

