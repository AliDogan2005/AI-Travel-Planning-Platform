package com.travelplanningplatform.repository;

import com.travelplanningplatform.entity.Notification;
import com.travelplanningplatform.entity.User;
import com.travelplanningplatform.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    List<Notification> findByRecipientAndTypeOrderByCreatedAtDesc(User recipient, NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.sent = false")
    List<Notification> findAllUnsent();

    long countByRecipientAndSent(User recipient, boolean sent);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < ?1")
    void deleteOldNotifications(LocalDateTime before);
}

