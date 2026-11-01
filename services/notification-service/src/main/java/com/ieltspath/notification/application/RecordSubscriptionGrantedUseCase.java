package com.ieltspath.notification.application;

import com.ieltspath.events.access.SubscriptionGrantedV1;
import com.ieltspath.notification.domain.repository.NotificationRepository;
import com.ieltspath.notification.domain.vo.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Stores the notice that a Premium subscription was granted to the learner. Delivery is at-least-once, so the source event
 * id makes the insert idempotent. The learner is recorded as the actor, since the notice names no other person.
 */
@Service
public class RecordSubscriptionGrantedUseCase {
    private final NotificationRepository notifications;

    public RecordSubscriptionGrantedUseCase(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /**
     * @param routingKey the routing key the message arrived with; must equal the event type
     * @param messageId  the broker message id; must equal the event id
     * @return the stored notification, or empty when it was a duplicate
     * @throws IllegalArgumentException when the event breaks the published contract
     */
    @Transactional
    public Optional<Notification> execute(SubscriptionGrantedV1 event, String routingKey, String messageId) {
        if (event == null || event.eventId() == null || event.userId() == null || event.occurredAt() == null
                || !SubscriptionGrantedV1.EVENT_TYPE.equals(event.eventType())
                || !event.eventType().equals(routingKey) || !event.eventId().toString().equals(messageId)) {
            throw new IllegalArgumentException("Unsupported subscription granted event");
        }
        var notification = new Notification(event.eventId(), event.userId(), event.userId(), event.eventType(),
                null, null, null, event.occurredAt(), null);
        return notifications.insertIfAbsent(event.eventId(), notification) ? Optional.of(notification) : Optional.empty();
    }
}
