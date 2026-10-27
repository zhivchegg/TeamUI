package com.teamui.service;

import com.teamui.domain.meeting.Meeting;
import com.teamui.domain.notification.Notification;
import com.teamui.domain.notification.NotificationRepository;
import com.teamui.domain.user.User;
import com.teamui.dto.NotificationResponse;
import com.teamui.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * In-app notifications about changes to meetings and action items made by other users.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Stores a notification for the recipient. Nothing is stored when the actor is the recipient:
     * nobody needs to be told about their own change.
     *
     * @param recipient the user to notify
     * @param actorId   the user who made the change
     * @param meeting   the meeting the change belongs to, or null
     * @param text      message in Russian, shown as is
     */
    @Transactional
    public void notify(User recipient, UUID actorId, Meeting meeting, String text) {
        if (recipient.getId().equals(actorId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setMeeting(meeting);
        notification.setText(text);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        Instant now = Instant.now();
        notificationRepository.findAllByUserIdAndReadAtIsNull(userId).forEach(n -> n.setReadAt(now));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getText(),
                n.getMeeting() == null ? null : n.getMeeting().getId(),
                n.getReadAt() != null,
                n.getCreatedAt()
        );
    }
}
