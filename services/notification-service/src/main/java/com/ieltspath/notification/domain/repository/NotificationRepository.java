package com.ieltspath.notification.domain.repository;

import com.ieltspath.notification.domain.vo.Notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository {
    /** Stores the notification unless one with the same source event already exists; true when a row was added. */
    boolean insertIfAbsent(UUID sourceEventId, Notification notification);

    List<Notification> findByRecipient(UUID recipientId, int size, long offset);

    long countUnread(UUID recipientId);

    /** Marks one notification of the recipient as read; false when it does not exist or belongs to someone else. */
    boolean markRead(UUID id, UUID recipientId, Instant readAt);

    int markAllRead(UUID recipientId, Instant readAt);
}
