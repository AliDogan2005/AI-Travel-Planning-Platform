package com.travelplanningplatform.service;

import com.travelplanningplatform.config.EmailConfig;
import com.travelplanningplatform.dto.EmailRequest;
import com.travelplanningplatform.dto.NotificationResponse;
import com.travelplanningplatform.entity.Notification;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.entity.enums.NotificationType;
import com.travelplanningplatform.repository.NotificationRepository;
import com.travelplanningplatform.repository.UserRepository;
import com.travelplanningplatform.util.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Send a registration welcome email
     */
    @Async
    @Transactional
    public void sendRegistrationEmail(User user) {
        try {
            String htmlContent = EmailTemplateBuilder.buildRegistrationEmail(user.getUsername());
            EmailRequest request = EmailRequest.builder()
                    .recipientEmail(user.getEmail())
                    .subject("Welcome to Travel Planning Platform!")
                    .body(htmlContent)
                    .type(NotificationType.REGISTRATION)
                    .recipientName(user.getUsername())
                    .build();

            sendEmail(request, user);
        } catch (Exception e) {
            log.error("Failed to send registration email to {}", user.getEmail(), e);
        }
    }

    /**
     * Send email when trip is created
     */
    @Async
    @Transactional
    public void sendTripCreatedEmail(User user, String tripName, String destination) {
        try {
            String htmlContent = EmailTemplateBuilder.buildTripCreatedEmail(user.getUsername(), tripName, destination);
            EmailRequest request = EmailRequest.builder()
                    .recipientEmail(user.getEmail())
                    .subject("Your New Trip: " + tripName + " has been created!")
                    .body(htmlContent)
                    .type(NotificationType.TRIP_CREATED)
                    .recipientName(user.getUsername())
                    .tripName(tripName)
                    .tripDestination(destination)
                    .build();

            sendEmail(request, user);
        } catch (Exception e) {
            log.error("Failed to send trip created email to {}", user.getEmail(), e);
        }
    }

    /**
     * Send email when itinerary is generated
     */
    @Async
    @Transactional
    public void sendItineraryGeneratedEmail(User user, String tripName) {
        try {
            String htmlContent = EmailTemplateBuilder.buildItineraryGeneratedEmail(user.getUsername(), tripName);
            EmailRequest request = EmailRequest.builder()
                    .recipientEmail(user.getEmail())
                    .subject("Your Itinerary for " + tripName + " is Ready!")
                    .body(htmlContent)
                    .type(NotificationType.ITINERARY_GENERATED)
                    .recipientName(user.getUsername())
                    .tripName(tripName)
                    .build();

            sendEmail(request, user);
        } catch (Exception e) {
            log.error("Failed to send itinerary generated email to {}", user.getEmail(), e);
        }
    }

    /**
     * Send budget alert email
     */
    @Async
    @Transactional
    public void sendBudgetAlertEmail(User user, String tripName, double budget, double spent) {
        try {
            String htmlContent = EmailTemplateBuilder.buildBudgetAlertEmail(user.getUsername(), tripName, budget, spent);
            EmailRequest request = EmailRequest.builder()
                    .recipientEmail(user.getEmail())
                    .subject("Budget Alert: " + tripName)
                    .body(htmlContent)
                    .type(NotificationType.BUDGET_ALERT)
                    .recipientName(user.getUsername())
                    .tripName(tripName)
                    .build();

            sendEmail(request, user);
        } catch (Exception e) {
            log.error("Failed to send budget alert email to {}", user.getEmail(), e);
        }
    }

    /**
     * Send trip reminder email
     */
    @Async
    @Transactional
    public void sendTripReminderEmail(User user, String tripName, String startDate) {
        try {
            String htmlContent = EmailTemplateBuilder.buildTripReminderEmail(user.getUsername(), tripName, startDate);
            EmailRequest request = EmailRequest.builder()
                    .recipientEmail(user.getEmail())
                    .subject("📅 Trip Reminder: " + tripName + " is coming up!")
                    .body(htmlContent)
                    .type(NotificationType.TRIP_REMINDER)
                    .recipientName(user.getUsername())
                    .tripName(tripName)
                    .build();

            sendEmail(request, user);
        } catch (Exception e) {
            log.error("Failed to send trip reminder email to {}", user.getEmail(), e);
        }
    }

    @Transactional
    protected void sendEmail(EmailRequest emailRequest, User recipient) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .recipientEmail(emailRequest.getRecipientEmail())
                .subject(emailRequest.getSubject())
                .body(emailRequest.getBody())
                .type(emailRequest.getType() != null ? emailRequest.getType() : NotificationType.GENERAL)
                .sent(false)
                .build();

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getMailFrom());
            helper.setTo(emailRequest.getRecipientEmail());
            helper.setSubject(emailRequest.getSubject());
            helper.setText(emailRequest.getBody(), true); // true = HTML

            javaMailSender.send(message);

            notification.setSent(true);
            notification.setSentAt(LocalDateTime.now());
            log.info("Email sent successfully to {}", emailRequest.getRecipientEmail());
        } catch (Exception e) {
            notification.setSendAttemptError(e.getMessage());
            log.error("Failed to send email to {}: {}", emailRequest.getRecipientEmail(), e.getMessage());
        }

        notificationRepository.save(notification);
    }

    /**
     * Retry sending failed notifications
     */
    @Async
    @Transactional
    public void retrySendingFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository.findAllUnsent();

        for (Notification notification : failedNotifications) {
            try {
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(emailConfig.getMailFrom());
                helper.setTo(notification.getRecipientEmail());
                helper.setSubject(notification.getSubject());
                helper.setText(notification.getBody(), true);

                javaMailSender.send(message);

                notification.setSent(true);
                notification.setSentAt(LocalDateTime.now());
                notification.setSendAttemptError(null);
                notificationRepository.save(notification);
                log.info("Successfully retried email to {}", notification.getRecipientEmail());
            } catch (Exception e) {
                notification.setSendAttemptError(e.getMessage());
                notificationRepository.save(notification);
                log.error("Retry failed for email to {}: {}", notification.getRecipientEmail(), e.getMessage());
            }
        }
    }

    /**
     * Get all notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications count
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationsCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.countByRecipientAndSent(user, true);
    }

    /**
     * Get notifications by type
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByType(Long userId, NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByRecipientAndTypeOrderByCreatedAtDesc(user, type)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Notification entity to DTO
     */
    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientEmail(notification.getRecipientEmail())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .type(notification.getType())
                .sent(notification.isSent())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .sendAttemptError(notification.getSendAttemptError())
                .build();
    }

    /**
     * Delete old notifications (older than 30 days)
     */
    @Async
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotifications(thirtyDaysAgo);
        log.info("Deleted notifications older than 30 days");
    }
}

