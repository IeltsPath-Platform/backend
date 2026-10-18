package com.ieltspath.notification.application;

import com.ieltspath.events.community.CommunityEventTypes;

import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a community event into a stored notification. Delivery is at-least-once, so the source event id makes
 * the insert idempotent; the recipient's preference decides whether anything is stored at all.
 */
@Service
public class RecordCommunityEventUseCase {
    private final NotificationRepository notifications;
    private final NotificationPreferenceService preferences;

    public RecordCommunityEventUseCase(NotificationRepository notifications, NotificationPreferenceService preferences) {
        this.notifications = notifications;
        this.preferences = preferences;
    }

    public record CommunityEvent(UUID eventId, String eventType, Instant occurredAt, UUID actorId, UUID recipientId,
                                 UUID postId, UUID commentId) { }

    /**
     * @param routingKey the routing key the message arrived with; must equal the event type
     * @param messageId  the broker message id; must equal the event id
     * @return the stored notification, or empty when it was a duplicate or the recipient disabled the type
     * @throws IllegalArgumentException when the event breaks the published contract
     */
    @Transactional
    public Optional<Notification> execute(CommunityEvent event, String routingKey, String messageId) {
        String type = event.eventType();
        if (!CommunityEventTypes.ALL.contains(type) || !type.equals(routingKey)
                || !event.eventId().toString().equals(messageId) || event.actorId().equals(event.recipientId()))
            throw new IllegalArgumentException("Unsupported community event");
        boolean postEvent = type.contains("commented") || type.contains("replied") || type.contains("mentioned") || type.contains("reacted");
        boolean commentEvent = type.contains("commented") || type.contains("replied") || type.contains("mentioned");
        if (postEvent && event.postId() == null) throw new IllegalArgumentException("Missing post reference");
        if (commentEvent && event.commentId() == null) throw new IllegalArgumentException("Missing comment reference");
        if (!preferences.isEnabled(event.recipientId(), type)) return Optional.empty();
        var notification = new Notification(event.eventId(), event.actorId(), event.recipientId(), type, event.postId(),
                event.commentId(), event.occurredAt(), null);
        return notifications.insertIfAbsent(event.eventId(), notification) ? Optional.of(notification) : Optional.empty();
    }
}
