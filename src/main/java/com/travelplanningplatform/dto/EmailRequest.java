package com.travelplanningplatform.dto;

import com.travelplanningplatform.entity.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRequest {
    private String recipientEmail;
    private String subject;
    private String body;
    private NotificationType type;
    private Long tripId;
    private String templateType;
    private String recipientName;
    private String tripName;
    private String tripDestination;
    private String sharedByUser;
}

