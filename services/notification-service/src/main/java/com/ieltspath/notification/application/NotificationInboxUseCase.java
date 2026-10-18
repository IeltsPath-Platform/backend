package com.ieltspath.notification.application;

import com.ieltspath.notification.domain.exception.NotificationNotFoundException;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reads and updates the signed-in recipient's own notifications. */
@Service
public class NotificationInboxUseCase {
    private static final int MAX_PAGE_SIZE = 100;
    private final NotificationRepository notifications;

    public NotificationInboxUseCase(NotificationRepository notifications) { this.notifications = notifications; }

    @Transactional(readOnly = true)
    public List<Notification> list(UUID recipientId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) throw new IllegalArgumentException("Invalid page or size");
        return notifications.findByRecipient(recipientId, size, (long) page * size);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID recipientId) { return notifications.countUnread(recipientId); }

    @Transactional
    public void markRead(UUID recipientId, UUID notificationId) {
        if (!notifications.markRead(notificationId, recipientId, Instant.now())) throw new NotificationNotFoundException();
    }

    @Transactional
    public int markAllRead(UUID recipientId) { return notifications.markAllRead(recipientId, Instant.now()); }
}
